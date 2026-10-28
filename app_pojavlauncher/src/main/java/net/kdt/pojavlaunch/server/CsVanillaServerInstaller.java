package net.kdt.pojavlaunch.server;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Downloads official Vanilla server.jar for a given MC version.
 */
public class CsVanillaServerInstaller {

    private static final String TAG = "CsVanillaInstaller";
    private static final String MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    public static class Result {
        public final boolean success;
        public final String error;
        public final File serverJar;

        public Result(boolean success, String error, File serverJar) {
            this.success = success;
            this.error = error;
            this.serverJar = serverJar;
        }
    }

    public Result install(CsServerConfig config) {
        File dir = new File(config.serverDir);
        if (!dir.exists() && !dir.mkdirs()) {
            return new Result(false, "Cannot create server dir", null);
        }
        File jar = new File(dir, "server.jar");
        try {
            String versionUrl = findVersionUrl(config.minecraftVersion);
            if (versionUrl == null) {
                return new Result(false, "Version not found in Mojang manifest: " + config.minecraftVersion, null);
            }
            String serverUrl = findServerDownloadUrl(versionUrl);
            if (serverUrl == null) {
                return new Result(false, "No server download for " + config.minecraftVersion, null);
            }
            downloadTo(serverUrl, jar);
            // Accept eula placeholder if needed later
            File eula = new File(dir, "eula.txt");
            if (!eula.exists()) {
                try (FileOutputStream fos = new FileOutputStream(eula)) {
                    fos.write("# By changing the setting below to TRUE you are indicating your agreement to our EULA.\neula=false\n".getBytes(StandardCharsets.UTF_8));
                }
            }
            return new Result(true, null, jar);
        } catch (Exception e) {
            Log.e(TAG, "Install failed", e);
            return new Result(false, e.getMessage(), null);
        }
    }

    private String findVersionUrl(String version) throws Exception {
        String json = httpGet(MANIFEST);
        JSONObject root = new JSONObject(json);
        JSONArray versions = root.getJSONArray("versions");
        for (int i = 0; i < versions.length(); i++) {
            JSONObject v = versions.getJSONObject(i);
            if (version.equals(v.optString("id"))) {
                return v.getString("url");
            }
        }
        return null;
    }

    private String findServerDownloadUrl(String versionJsonUrl) throws Exception {
        String json = httpGet(versionJsonUrl);
        JSONObject root = new JSONObject(json);
        JSONObject downloads = root.optJSONObject("downloads");
        if (downloads == null) return null;
        JSONObject server = downloads.optJSONObject("server");
        if (server == null) return null;
        return server.optString("url", null);
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "CS-Launcher/3.0");
        try (InputStream in = conn.getInputStream()) {
            byte[] buf = new byte[8192];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = in.read(buf)) > 0) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private void downloadTo(String urlStr, File dest) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(120000);
        conn.setRequestProperty("User-Agent", "CS-Launcher/3.0");
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        } finally {
            conn.disconnect();
        }
    }
}
