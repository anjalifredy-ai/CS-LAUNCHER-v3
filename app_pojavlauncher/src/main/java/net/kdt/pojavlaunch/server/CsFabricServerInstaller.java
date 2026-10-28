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
 * Installs Fabric dedicated server via official Fabric meta API.
 * https://meta.fabricmc.net/v2/versions/loader/{mc}/{loader}/1.0.0/server/jar
 */
public class CsFabricServerInstaller {

    private static final String TAG = "CsFabricInstaller";
    private static final String META = "https://meta.fabricmc.net/v2";

    public static class Result {
        public final boolean success;
        public final String error;
        public final File serverJar;
        public final String loaderVersion;

        public Result(boolean success, String error, File serverJar, String loaderVersion) {
            this.success = success;
            this.error = error;
            this.serverJar = serverJar;
            this.loaderVersion = loaderVersion;
        }
    }

    public Result install(CsServerConfig config) {
        File dir = new File(config.serverDir);
        if (!dir.exists() && !dir.mkdirs()) {
            return new Result(false, "Cannot create server dir", null, null);
        }
        new File(dir, "mods").mkdirs();
        File jar = new File(dir, "server.jar");

        try {
            String loader = getLatestLoader(config.minecraftVersion);
            if (loader == null) {
                return new Result(false, "No Fabric loader for " + config.minecraftVersion, null, null);
            }
            String jarUrl = META + "/versions/loader/" + config.minecraftVersion + "/" + loader + "/1.0.0/server/jar";
            downloadTo(jarUrl, jar);
            if (!jar.exists() || jar.length() == 0) {
                return new Result(false, "Fabric server jar empty", null, null);
            }
            File eula = new File(dir, "eula.txt");
            if (!eula.exists()) {
                try (FileOutputStream fos = new FileOutputStream(eula)) {
                    fos.write("eula=false\n".getBytes(StandardCharsets.UTF_8));
                }
            }
            return new Result(true, null, jar, loader);
        } catch (Exception e) {
            Log.e(TAG, "Fabric install failed", e);
            return new Result(false, e.getMessage(), null, null);
        }
    }

    private String getLatestLoader(String mcVersion) throws Exception {
        String json = httpGet(META + "/versions/loader/" + mcVersion);
        JSONArray arr = new JSONArray(json);
        if (arr.length() == 0) return null;
        JSONObject first = arr.getJSONObject(0);
        JSONObject loader = first.optJSONObject("loader");
        if (loader != null) return loader.optString("version", null);
        return first.optString("version", null);
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
