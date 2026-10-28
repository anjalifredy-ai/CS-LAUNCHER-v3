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
 * Downloads Paper server jar via Paper API.
 */
public class CsPaperServerInstaller {

    private static final String TAG = "CsPaperInstaller";
    private static final String API = "https://api.papermc.io/v2/projects/paper";

    public static class Result {
        public final boolean success;
        public final String error;
        public final File serverJar;
        public final int buildNumber;

        public Result(boolean success, String error, File serverJar, int buildNumber) {
            this.success = success;
            this.error = error;
            this.serverJar = serverJar;
            this.buildNumber = buildNumber;
        }
    }

    public Result install(CsServerConfig config) {
        File dir = new File(config.serverDir);
        if (!dir.exists() && !dir.mkdirs()) {
            return new Result(false, "Cannot create server dir", null, 0);
        }
        File jar = new File(dir, "server.jar");
        try {
            String ver = config.minecraftVersion;
            String buildsJson = httpGet(API + "/versions/" + ver);
            JSONObject buildsRoot = new JSONObject(buildsJson);
            JSONArray builds = buildsRoot.getJSONArray("builds");
            if (builds.length() == 0) {
                return new Result(false, "No Paper builds for " + ver, null, 0);
            }
            int build = builds.getInt(builds.length() - 1);
            String metaJson = httpGet(API + "/versions/" + ver + "/builds/" + build);
            JSONObject meta = new JSONObject(metaJson);
            JSONObject downloads = meta.getJSONObject("downloads");
            JSONObject application = downloads.getJSONObject("application");
            String name = application.getString("name");
            String downloadUrl = API + "/versions/" + ver + "/builds/" + build + "/downloads/" + name;
            downloadTo(downloadUrl, jar);

            File eula = new File(dir, "eula.txt");
            if (!eula.exists()) {
                try (FileOutputStream fos = new FileOutputStream(eula)) {
                    fos.write("eula=false\n".getBytes(StandardCharsets.UTF_8));
                }
            }
            return new Result(true, null, jar, build);
        } catch (Exception e) {
            Log.e(TAG, "Paper install failed", e);
            return new Result(false, e.getMessage(), null, 0);
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
