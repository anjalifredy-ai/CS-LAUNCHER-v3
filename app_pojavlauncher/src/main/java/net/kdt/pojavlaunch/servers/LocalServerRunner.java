package net.kdt.pojavlaunch.servers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.server.ProcessCompat;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.server.CsJavaPathResolver;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
        Process p = RUNNING.remove(id);
        if (p != null) {
            try {
                p.getOutputStream().write("stop\n".getBytes(StandardCharsets.UTF_8));
                p.getOutputStream().flush();
                long waited = 0;
                while (ProcessCompat.isAlive(p) && waited < 12000) { Thread.sleep(400); waited += 400; }
            } catch (Throwable ignored) {}
            try { if (ProcessCompat.isAlive(p)) ProcessCompat.destroyForcibly(p); } catch (Throwable ignored) {}
            try { p.waitFor(); } catch (InterruptedException ignored) {}
        }
    }

    public static void stopAll() {
        for (String id : new ArrayList<>(RUNNING.keySet())) stop(id);
    }

    public static void sendCommand(String id, String command) {
        Process p = RUNNING.get(id);
        if (p == null) return;
        try {
            p.getOutputStream().write((command + "\n").getBytes(StandardCharsets.UTF_8));
            p.getOutputStream().flush();
        } catch (Throwable t) { Log.e(TAG, "sendCommand", t); }
    }

    public static void prepare(Context ctx, LocalServerEntry e) throws Exception {
        File dir = serverDir(ctx, e);
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Cannot create server folder");
        File jar = new File(dir, "server.jar");
        String soft = e.software != null ? e.software.toLowerCase() : "vanilla";
        if (!jar.exists() || jar.length() < 1000) {
            if ("paper".equals(soft)) downloadPaper(e.versionId, jar);
            else if ("purpur".equals(soft)) downloadPurpur(e.versionId, jar);
            else if ("fabric".equals(soft)) { downloadFabric(e.versionId, jar); new File(dir, "mods").mkdirs(); }
            else {
                String url = resolveServerJarUrl(e.versionId);
                if (url == null) throw new Exception("Server jar URL not found for " + e.versionId);
                DownloadUtils.downloadFile(url, jar);
            }
        }
        File eula = new File(dir, "eula.txt");
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(eula), StandardCharsets.UTF_8)) {
            w.write("# Accepted in CS Launcher\neula=true\n");
        }
        writeProperties(dir, e);
        writeServerIcon(dir, e);
    }

    private static void writeProperties(File dir, LocalServerEntry e) throws Exception {
        File props = new File(dir, "server.properties");
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(props), StandardCharsets.UTF_8)) {
            w.write("# CS Launcher local server\n");
            w.write("server-port=" + e.port + "\n");
            w.write("server-ip=0.0.0.0\n");
            w.write("max-players=" + e.maxPlayers + "\n");
            w.write("online-mode=" + e.onlineMode + "\n");
            w.write("white-list=" + e.whitelist + "\n");
            String motd = e.motdLine().replace("\n", " ").replace("=", "-");
            w.write("motd=" + motd + "\n");
            String diff = e.difficulty != null ? e.difficulty : "normal";
            String gm = e.gamemode != null ? e.gamemode : "survival";
            w.write("difficulty=" + diff + "\n");
            w.write("gamemode=" + gm + "\n");
            w.write("pvp=" + e.pvp + "\n");
            w.write("hardcore=" + e.hardcore + "\n");
            w.write("allow-flight=" + e.allowFlight + "\n");
            w.write("spawn-protection=0\n");
            int vd = e.viewDistance > 0 ? e.viewDistance : 8;
            int sd = e.simulationDistance > 0 ? e.simulationDistance : 6;
            w.write("view-distance=" + vd + "\n");
            w.write("simulation-distance=" + sd + "\n");
            String level = e.worldName != null && !e.worldName.isEmpty() ? e.worldName : "world";
            w.write("level-name=" + level + "\n");
            w.write("max-tick-time=60000\n");
            w.write("enable-status=true\n");
            w.write("hide-online-players=false\n");
        }
    }

    public static void applySettings(Context ctx, LocalServerEntry e) throws Exception {
        File dir = serverDir(ctx, e);
        if (!dir.exists()) dir.mkdirs();
        writeProperties(dir, e);
        writeServerIcon(dir, e);
    }

    public static void writeServerIcon(File dir, LocalServerEntry e) {
        try {
            File dest = new File(dir, "server-icon.png");
            Bitmap src = null;
            if (e.iconPath != null) {
                File f = new File(e.iconPath);
                if (f.exists()) src = BitmapFactory.decodeFile(f.getAbsolutePath());
            }
            if (src == null) {
                src = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(src);
                c.drawColor(0xFF1B5E20);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(0xFFFFEB3B);
                p.setTextSize(22f);
                p.setTextAlign(Paint.Align.CENTER);
                String letter = e.name != null && !e.name.isEmpty() ? e.name.substring(0, 1).toUpperCase() : "S";
                c.drawText(letter, 32, 40, p);
            } else if (src.getWidth() != 64 || src.getHeight() != 64) {
                src = Bitmap.createScaledBitmap(src, 64, 64, true);
            }
            try (FileOutputStream out = new FileOutputStream(dest)) {
                src.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        } catch (Throwable t) { Log.w(TAG, "server-icon write failed", t); }
    }

    public static void setIconFromFile(Context ctx, LocalServerEntry e, File png) throws Exception {
        if (png == null || !png.exists()) throw new Exception("Icon file missing");
        Bitmap b = BitmapFactory.decodeFile(png.getAbsolutePath());
        if (b == null) throw new Exception("Not a valid image");
        File dir = serverDir(ctx, e);
        dir.mkdirs();
        File stored = new File(dir, "custom-icon-src.png");
        try (FileInputStream in = new FileInputStream(png); FileOutputStream out = new FileOutputStream(stored)) {
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        e.iconPath = stored.getAbsolutePath();
        writeServerIcon(dir, e);
        LocalServerStore.update(ctx, e);
    }

    public static void start(Context ctx, LocalServerEntry e) throws Exception {
        if (isRunning(e.id)) return;
        prepare(ctx, e);
        File dir = serverDir(ctx, e);
        File jar = new File(dir, "server.jar");
        if (!jar.exists()) throw new Exception("server.jar missing");
        String javaBin = null;
        try { javaBin = CsJavaPathResolver.resolveForMinecraftVersion(e.versionId); } catch (Throwable t) { Log.w(TAG, "CsJavaPathResolver failed", t); }
        if (javaBin == null) javaBin = findJavaBinary();
        if (javaBin == null) throw new Exception(CsJavaPathResolver.missingRuntimeMessage(e.versionId != null ? e.versionId : "1.20.4"));
        int mem = e.memoryMb > 0 ? e.memoryMb : 1024;
        if (mem < 512) mem = 512; if (mem > 4096) mem = 4096;
        int xms = Math.max(mem / 2, 256);
        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin); cmd.add("-Xms" + xms + "M"); cmd.add("-Xmx" + mem + "M");
        cmd.add("-XX:+UseG1GC"); cmd.add("-XX:+ParallelRefProcEnabled"); cmd.add("-Djava.awt.headless=true");
        cmd.add("-jar"); cmd.add(jar.getAbsolutePath()); cmd.add("nogui");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir); pb.redirectErrorStream(true);
        Process p = pb.start();
        RUNNING.put(e.id, p);
        LOGS.put(e.id, new CopyOnWriteArrayList<>());
        final String sid = e.id; final String sname = e.name;
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Log.i(TAG, "[" + sname + "] " + line);
                    List<String> buf = LOGS.get(sid);
                    if (buf != null) { buf.add(line); while (buf.size() > 400) buf.remove(0); }
                    for (LogListener l : LOG_LISTENERS) { try { l.onLine(sid, line); } catch (Throwable ignored) {} }
                }
            } catch (Throwable ignored) {}
            finally { RUNNING.remove(sid); }
        }, "cs-server-" + e.id).start();
        e.running = true;
        try {
            net.kdt.pojavlaunch.servers.connectivity.ConnectivityManager.afterServerStart(ctx, e);
        } catch (Throwable t) {
            Log.w(TAG, "connectivity", t);
            try { PlayitAccountStore.applyDefaultToServer(ctx, e); } catch (Throwable ignored) {}
        }
        LocalServerStore.update(ctx, e);
        try { LocalServerService.start(ctx, e.name, e.port); } catch (Throwable t) { Log.w(TAG, "FGS start skipped", t); }
    }

    private static String findJavaBinary() {
        try {
            List<Runtime> rts = MultiRTUtils.getInstalledRuntimes();
            Runtime best = null;
            for (Runtime r : rts) {
                if (r == null || r.name == null || r.javaVersion <= 0) continue;
                if (best == null || r.javaVersion > best.javaVersion) best = r;
            }
            if (best == null) return null;
            File home = new File(Tools.MULTIRT_HOME, best.name);
            for (File f : new File[]{ new File(home, "bin/java"), new File(home, "jre/bin/java") }) {
                if (f.exists() && f.canExecute()) return f.getAbsolutePath();
            }
        } catch (Throwable t) { Log.e(TAG, "findJavaBinary", t); }
        return null;
    }

    public static String resolveServerJarUrl(String versionId) {
        try {
            String manifest = httpGet("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
            if (manifest == null) return null;
            JSONObject root = new JSONObject(manifest);
            org.json.JSONArray versions = root.getJSONArray("versions");
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
        String body = httpGet("https://api.papermc.io/v2/projects/paper/versions/" + mc);
        if (body == null) throw new Exception("Paper API failed for " + mc);
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
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Throwable t) { return null; }
        finally { if (c != null) c.disconnect(); }
    }
}
