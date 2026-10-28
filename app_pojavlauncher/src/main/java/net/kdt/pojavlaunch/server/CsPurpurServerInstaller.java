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
 * Downloads Purpur server jar from https://api.purpurmc.org
 */
public class CsPurpurServerInstaller {

    private static final String TAG = "CsPurpurInstaller";
    private static final String API = "https://api.purpurmc.org/v2/purpur";

    public static class Result {
        public final boolean success;
        public final String message;
        public final File serverJar;

        public Result(boolean success, String message, File serverJar) {
            this.success = success;
            this.message = message;
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
            String ver = config.minecraftVersion;
            String metaJson = httpGet(API + "/" + ver);
            JSONObject meta = new JSONObject(metaJson);
            JSONObject builds = meta.getJSONObject("builds");
            String latest = builds.getString("latest");
            String downloadUrl = API + "/" + ver + "/" + latest + "/download";
            downloadTo(downloadUrl, jar);
            File eula = new File(dir, "eula.txt");
            if (!eula.exists()) {
                try (FileOutputStream fos = new FileOutputStream(eula)) {
                    fos.write("eula=false\n".getBytes(StandardCharsets.UTF_8));
                }
            }
            return new Result(true, "Purpur build " + latest + " installed", jar);
        } catch (Exception e) {
            Log.e(TAG, "Purpur install failed", e);
            return new Result(false, e.getMessage(), null);
        }
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
