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
 * Installs ViaVersion (and optionally ViaBackwards/ViaRewind) from Modrinth into plugins/.
 * Enables cross-version client connections on Paper/Purpur.
 */
public class CsViaVersionInstaller {

    private static final String TAG = "CsViaVersion";
    private static final String MODRINTH = "https://api.modrinth.com/v2";

    public static class Result {
        public final boolean success;
        public final String message;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public Result installForCrossVersion(CsServerConfig config) {
        File plugins = new File(config.serverDir, "plugins");
        if (!plugins.exists() && !plugins.mkdirs()) {
            return new Result(false, "Cannot create plugins dir");
        }
        try {
            // ViaVersion project id on Modrinth
            String via = downloadLatestPlugin("P1CA4riY", plugins, "ViaVersion.jar"); // ViaVersion
            String msg = "ViaVersion: " + via;
            // ViaBackwards
            try {
                String vb = downloadLatestPlugin("NpvuJRoT", plugins, "ViaBackwards.jar");
                msg += "; ViaBackwards: " + vb;
            } catch (Exception e) {
                msg += "; ViaBackwards skipped: " + e.getMessage();
            }
            return new Result(true, msg);
        } catch (Exception e) {
            Log.e(TAG, "ViaVersion install failed", e);
            return new Result(false, e.getMessage());
        }
    }

    private String downloadLatestPlugin(String projectId, File pluginsDir, String fileName) throws Exception {
        String versionsJson = httpGet(MODRINTH + "/project/" + projectId + "/version?loaders=[\"paper\",\"bukkit\",\"spigot\"]&limit=5");
        JSONArray arr = new JSONArray(versionsJson);
        if (arr.length() == 0) throw new Exception("No versions for " + projectId);
        JSONObject ver = arr.getJSONObject(0);
        JSONArray files = ver.getJSONArray("files");
        String url = null;
        for (int i = 0; i < files.length(); i++) {
            JSONObject f = files.getJSONObject(i);
            if (f.optBoolean("primary", false) || url == null) {
                url = f.getString("url");
            }
        }
        if (url == null) throw new Exception("No file url");
        File dest = new File(pluginsDir, fileName);
        downloadTo(url, dest);
        return ver.optString("version_number", fileName);
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
            while ((n = in.read(buf)) > 0) sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
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
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } finally {
            conn.disconnect();
        }
    }
}
