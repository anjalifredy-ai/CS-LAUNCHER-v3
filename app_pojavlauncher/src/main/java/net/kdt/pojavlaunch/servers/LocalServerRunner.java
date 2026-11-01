package net.kdt.pojavlaunch.servers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.server.CsJavaPathResolver;
import net.kdt.pojavlaunch.server.ProcessCompat;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Runs a real Minecraft dedicated server on-device using installed MultiRT JRE. */
public final class LocalServerRunner {
    private static final String TAG = "LocalServerRunner";
    private static final ConcurrentHashMap<String, Process> RUNNING = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<String>> LOGS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<LogListener> LOG_LISTENERS = new CopyOnWriteArrayList<>();

    public interface LogListener { void onLine(String serverId, String line); }

    private LocalServerRunner() {}

    public static void addLogListener(LogListener l) { LOG_LISTENERS.add(l); }
    public static void removeLogListener(LogListener l) { LOG_LISTENERS.remove(l); }

    public static List<String> getLogSnapshot(String id) {
        List<String> l = LOGS.get(id);
        return l != null ? new ArrayList<>(l) : new ArrayList<>();
    }

    public static File serverDir(Context ctx, LocalServerEntry e) {
        return new File(Tools.DIR_GAME_NEW, "remom_servers/" + e.id);
    }

    public static boolean isRunning(String id) {
        Process p = RUNNING.get(id);
        if (p == null) return false;
        try { p.exitValue(); RUNNING.remove(id); return false;
        } catch (IllegalThreadStateException stillRunning) { return true; }
    }

    public static void stop(String id) {
        try {
            net.kdt.pojavlaunch.servers.connectivity.BoreTunnelProvider.stopAgent(id);
        } catch (Throwable ignored) {}
        Process p = RUNNING.remove(id);
        if (p == null) return;
        try {
            try {
                p.getOutputStream().write("stop\n".getBytes(StandardCharsets.UTF_8));
                p.getOutputStream().flush();
                Thread.sleep(1500);
            } catch (Throwable ignored) {}
            ProcessCompat.destroy(p);
        } catch (Throwable t) { Log.w(TAG, "stop", t); }
    }

    public static void stopAll() {
        try {
            net.kdt.pojavlaunch.servers.connectivity.BoreTunnelProvider.stopAll();
        } catch (Throwable ignored) {}
        for (String id : new ArrayList<>(RUNNING.keySet())) stop(id);
    }

    public static void sendCommand(String id, String cmd) throws Exception {
        Process p = RUNNING.get(id);
        if (p == null) throw new Exception("Server not running");
        p.getOutputStream().write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
        p.getOutputStream().flush();
    }

    public static void ensureInstalled(Context ctx, LocalServerEntry e) throws Exception {
        File dir = serverDir(ctx, e);
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Cannot create server dir");
        File jar = new File(dir, "server.jar");
        if (!jar.exists() || jar.length() < 1000) {
            String soft = e.software != null ? e.software.toLowerCase() : "vanilla";
            if ("paper".equals(soft)) downloadPaper(e.versionId, jar);
            else if ("purpur".equals(soft)) downloadPurpur(e.versionId, jar);
            else if ("fabric".equals(soft)) { downloadFabric(e.versionId, jar); new File(dir, "mods").mkdirs(); }
            else {
                String url = resolveVanillaServerJarUrl(e.versionId);
                if (url == null) throw new Exception("Cannot resolve server jar for " + e.versionId);
                DownloadUtils.downloadFile(url, jar);
            }
        }
        writeEula(dir, true);
        writeServerProperties(dir, e);
        try { writeServerIcon(dir); } catch (Throwable t) { Log.w(TAG, "icon", t); }
    }

    public static void start(Context ctx, LocalServerEntry e) throws Exception {
        if (isRunning(e.id)) return;
        ensureInstalled(ctx, e);
        File dir = serverDir(ctx, e);
        File jar = new File(dir, "server.jar");
        if (!jar.exists()) throw new Exception("server.jar missing");
        String javaBin = null;
        try { javaBin = CsJavaPathResolver.resolveForMinecraftVersion(e.versionId); } catch (Throwable t) { Log.w(TAG, "CsJavaPathResolver failed", t); }
        if (javaBin == null) javaBin = findJavaBinary();
        if (javaBin == null) throw new Exception(CsJavaPathResolver.missingRuntimeMessage(e.versionId != null ? e.versionId : "1.20.4"));
        int mem = Math.max(256, e.memoryMb);
        int xms = Math.min(mem, Math.max(128, mem / 2));
        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin); cmd.add("-Xms" + xms + "M"); cmd.add("-Xmx" + mem + "M");
        cmd.add("-XX:+UseG1GC"); cmd.add("-XX:+ParallelRefProcEnabled"); cmd.add("-Djava.awt.headless=true");
        cmd.add("-jar"); cmd.add(jar.getAbsolutePath()); cmd.add("nogui");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir); pb.redirectErrorStream(true);
        Process p = pb.start();
        RUNNING.put(e.id, p);
        LOGS.putIfAbsent(e.id, new CopyOnWriteArrayList<>());
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    List<String> log = LOGS.get(e.id);
                    if (log != null) {
                        log.add(line);
                        if (log.size() > 2000) log.remove(0);
                    }
                    for (LogListener l : LOG_LISTENERS) {
                        try { l.onLine(e.id, line); } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable t) { Log.w(TAG, "log reader", t); }
            finally { RUNNING.remove(e.id); }
        }, "cs-server-" + e.id).start();
        try { LocalServerService.start(ctx, e.name, e.port); } catch (Throwable t) { Log.w(TAG, "FGS start skipped", t); }
        try { net.kdt.pojavlaunch.servers.connectivity.ConnectivityManager.afterServerStart(ctx, e); } catch (Throwable t) { Log.w(TAG, "connectivity", t); }
    }

    private static String findJavaBinary() {
        try {
            Runtime best = null;
            for (Runtime r : MultiRTUtils.getRuntimes()) {
                if (r == null || r.name == null || r.javaVersion <= 0) continue;
                if (best == null || r.javaVersion > best.javaVersion) best = r;
            }
            if (best == null) return null;
            File home = MultiRTUtils.getRuntimeHome(best.name);
            for (File f : new File[]{ new File(home, "bin/java"), new File(home, "jre/bin/java") }) {
                if (f.exists() && f.canExecute()) return f.getAbsolutePath();
            }
        } catch (Throwable t) { Log.w(TAG, "findJavaBinary", t); }
        return null;
    }

    private static void writeEula(File dir, boolean accepted) throws Exception {
        File eula = new File(dir, "eula.txt");
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(eula), StandardCharsets.UTF_8)) {
            w.write("eula=" + (accepted ? "true" : "false") + "\n");
        }
    }

    private static void writeServerProperties(File dir, LocalServerEntry e) throws Exception {
        File props = new File(dir, "server.properties");
        StringBuilder sb = new StringBuilder();
        sb.append("server-port=").append(e.port).append('\n');
        sb.append("max-players=").append(e.maxPlayers).append('\n');
        sb.append("online-mode=").append(e.onlineMode ? "true" : "false").append('\n');
        sb.append("motd=").append(e.motdLine().replace("\n", "\\n")).append('\n');
        sb.append("view-distance=").append(e.viewDistance > 0 ? e.viewDistance : 8).append('\n');
        sb.append("simulation-distance=").append(e.simulationDistance > 0 ? e.simulationDistance : 6).append('\n');
        sb.append("spawn-protection=0\n");
        sb.append("difficulty=normal\n");
        sb.append("gamemode=survival\n");
        sb.append("enable-command-block=true\n");
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(props), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
    }

    private static void writeServerIcon(File dir) throws Exception {
        File icon = new File(dir, "server-icon.png");
        if (icon.exists()) return;
        Bitmap bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint();
        p.setColor(0xFF1A2332);
        c.drawRect(0, 0, 64, 64, p);
        p.setColor(0xFF67A8FF);
        c.drawCircle(32, 32, 20, p);
        try (FileOutputStream fos = new FileOutputStream(icon)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        bmp.recycle();
    }

    private static String resolveVanillaServerJarUrl(String versionId) {
        try {
            String manifest = httpGet("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
            if (manifest == null) return null;
            org.json.JSONArray versions = new JSONObject(manifest).getJSONArray("versions");
            String versionUrl = null;
            for (int i = 0; i < versions.length(); i++) {
                JSONObject v = versions.getJSONObject(i);
                if (versionId.equals(v.optString("id"))) { versionUrl = v.optString("url"); break; }
            }
            if (versionUrl == null || versionUrl.isEmpty()) return null;
            String verJson = httpGet(versionUrl);
            if (verJson == null) return null;
            JSONObject downloads = new JSONObject(verJson).optJSONObject("downloads");
            if (downloads == null) return null;
            JSONObject server = downloads.optJSONObject("server");
            if (server == null) return null;
            return server.optString("url", null);
        } catch (Throwable t) { Log.e(TAG, "resolveServerJarUrl", t); return null; }
    }

    private static void downloadPaper(String mc, File dest) throws Exception {
        Exception last = null;
        try {
            String fill = "https://fill.papermc.io/v3/projects/paper/versions/" + mc + "/builds/latest/downloads/server:default";
            DownloadUtils.downloadFile(fill, dest);
            if (dest.exists() && dest.length() > 1000) return;
        } catch (Exception e) { last = e; Log.w(TAG, "Paper fill v3 failed", e); }
        String body = httpGet("https://api.papermc.io/v2/projects/paper/versions/" + mc);
        if (body == null) throw last != null ? last : new Exception("Paper API failed for " + mc);
        org.json.JSONArray builds = new JSONObject(body).getJSONArray("builds");
        if (builds.length() == 0) throw new Exception("No Paper builds for " + mc);
        int build = builds.getInt(builds.length() - 1);
        String url = "https://api.papermc.io/v2/projects/paper/versions/" + mc + "/builds/" + build + "/downloads/paper-" + mc + "-" + build + ".jar";
        DownloadUtils.downloadFile(url, dest);
    }

    private static void downloadPurpur(String mc, File dest) throws Exception {
        DownloadUtils.downloadFile("https://api.purpurmc.org/v2/purpur/" + mc + "/latest/download", dest);
    }

    private static void downloadFabric(String mc, File dest) throws Exception {
        String meta = httpGet("https://meta.fabricmc.net/v2/versions/loader/" + mc);
        if (meta == null) throw new Exception("Fabric meta failed");
        org.json.JSONArray arr = new org.json.JSONArray(meta);
        if (arr.length() == 0) throw new Exception("No Fabric loader for " + mc);
        String loader = arr.getJSONObject(0).getJSONObject("loader").getString("version");
        DownloadUtils.downloadFile("https://meta.fabricmc.net/v2/versions/loader/" + mc + "/" + loader + "/1.0.0/server/jar", dest);
    }

    private static String httpGet(String urlStr) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setConnectTimeout(20000); c.setReadTimeout(45000);
            c.setRequestProperty("User-Agent", "CS-Launcher/3");
            try (InputStream in = c.getInputStream(); BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        } catch (Throwable t) {
            Log.w(TAG, "httpGet " + urlStr, t);
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
