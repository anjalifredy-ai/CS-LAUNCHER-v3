package net.kdt.pojavlaunch.servers;

import android.content.Context;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.Runtime;
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

/**
 * Runs a lightweight vanilla dedicated server inside Remom using the installed JRE.
 * Friends join via LAN IP:port — no Aternos/Minehut website.
 */
public final class LocalServerRunner {
    private static final String TAG = "LocalServerRunner";
    private static final ConcurrentHashMap<String, Process> RUNNING = new ConcurrentHashMap<>();

    private LocalServerRunner() {}

    public static File serverDir(Context ctx, LocalServerEntry e) {
        return new File(Tools.DIR_GAME_NEW, "remom_servers/" + e.id);
    }

    public static boolean isRunning(String id) {
        Process p = RUNNING.get(id);
        if (p == null) return false;
        try {
            p.exitValue();
            RUNNING.remove(id);
            return false;
        } catch (IllegalThreadStateException stillRunning) {
            return true;
        }
    }

    public static void stop(String id) {
        Process p = RUNNING.remove(id);
        if (p != null) {
            try {
                p.getOutputStream().write("stop\n".getBytes(StandardCharsets.UTF_8));
                p.getOutputStream().flush();
            } catch (Throwable ignored) {}
            p.destroy();
            try { p.waitFor(); } catch (InterruptedException ignored) {}
        }
    }

    public static void stopAll() {
        for (String id : new ArrayList<>(RUNNING.keySet())) stop(id);
    }

    /** Ensure server jar + eula + properties exist for this entry. */
    public static void prepare(Context ctx, LocalServerEntry e) throws Exception {
        File dir = serverDir(ctx, e);
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Cannot create server folder");

        File jar = new File(dir, "server.jar");
        if (!jar.exists() || jar.length() < 1000) {
            String url = resolveServerJarUrl(e.versionId);
            if (url == null) throw new Exception("Server jar URL not found for " + e.versionId);
            DownloadUtils.downloadFile(url, jar);
        }

        File eula = new File(dir, "eula.txt");
        if (!eula.exists()) {
            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(eula), StandardCharsets.UTF_8)) {
                w.write("eula=true\n");
            }
        }

        File props = new File(dir, "server.properties");
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(props), StandardCharsets.UTF_8)) {
            w.write("server-port=" + e.port + "\n");
            w.write("max-players=" + e.maxPlayers + "\n");
            w.write("online-mode=" + e.onlineMode + "\n");
            w.write("motd=" + (e.name != null ? e.name.replace("=", " ") : "Remom Server") + "\n");
            w.write("difficulty=normal\n");
            w.write("gamemode=survival\n");
            w.write("spawn-protection=0\n");
            w.write("view-distance=8\n");
            w.write("simulation-distance=6\n");
            w.write("max-tick-time=60000\n");
        }
    }

    public static void start(Context ctx, LocalServerEntry e) throws Exception {
        if (isRunning(e.id)) return;
        prepare(ctx, e);

        File dir = serverDir(ctx, e);
        File jar = new File(dir, "server.jar");
        if (!jar.exists()) throw new Exception("server.jar missing");

        String javaBin = findJavaBinary();
        if (javaBin == null) throw new Exception("Install a Java runtime in Settings first");

        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        cmd.add("-Xms256M");
        cmd.add("-Xmx768M");
        cmd.add("-jar");
        cmd.add(jar.getAbsolutePath());
        cmd.add("nogui");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        RUNNING.put(e.id, p);

        // Drain log so process does not block
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Log.i(TAG, "[" + e.name + "] " + line);
                }
            } catch (Throwable ignored) {}
        }, "remom-server-" + e.id).start();

        e.running = true;
        LocalServerStore.update(ctx, e);
    }

    private static String findJavaBinary() {
        try {
            List<Runtime> rts = MultiRTUtils.getInstalledRuntimes();
            for (Runtime r : rts) {
                if (r == null || r.name == null) continue;
                File home = new File(Tools.MULTIRT_HOME, r.name);
                File[] candidates = new File[]{
                        new File(home, "bin/java"),
                        new File(home, "jre/bin/java")
                };
                for (File f : candidates) {
                    if (f.exists() && f.canExecute()) return f.getAbsolutePath();
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "findJavaBinary", t);
        }
        return null;
    }

    /** Resolve official server jar URL from Mojang version manifest. */
    public static String resolveServerJarUrl(String versionId) {
        try {
            String manifest = httpGet("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
            if (manifest == null) return null;
            JSONObject root = new JSONObject(manifest);
            org.json.JSONArray versions = root.getJSONArray("versions");
            String versionUrl = null;
            for (int i = 0; i < versions.length(); i++) {
                JSONObject v = versions.getJSONObject(i);
                if (versionId.equals(v.optString("id"))) {
                    versionUrl = v.optString("url");
                    break;
                }
            }
            if (versionUrl == null || versionUrl.isEmpty()) return null;
            String verJson = httpGet(versionUrl);
            if (verJson == null) return null;
            JSONObject downloads = new JSONObject(verJson).optJSONObject("downloads");
            if (downloads == null) return null;
            JSONObject server = downloads.optJSONObject("server");
            if (server == null) return null;
            return server.optString("url", null);
        } catch (Throwable t) {
            Log.e(TAG, "resolveServerJarUrl", t);
            return null;
        }
    }

    private static String httpGet(String urlStr) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent", "RemomLauncher");
            try (InputStream in = c.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Throwable t) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
