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
 * Downloads Paper server jar from PaperMC API for a given MC version.
 */
public class CsPaperInstaller {

    private static final String TAG = "CsPaperInstaller";

    public static File install(CsServerConfig config) throws Exception {
        File dir = new File(config.serverDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Cannot create server dir");
        }

        String version = config.minecraftVersion;
        String buildsUrl = "https://api.papermc.io/v2/projects/paper/versions/" + version;
        JSONObject buildsJson = new JSONObject(httpGet(buildsUrl));
        JSONArray builds = buildsJson.getJSONArray("builds");
        if (builds.length() == 0) {
            throw new IllegalStateException("No Paper builds for " + version);
        }
        int build = builds.getInt(builds.length() - 1);

        String downloadUrl = "https://api.papermc.io/v2/projects/paper/versions/" + version
                + "/builds/" + build + "/downloads/paper-" + version + "-" + build + ".jar";

        File out = new File(dir, "paper-" + version + "-" + build + ".jar");
        downloadTo(downloadUrl, out);
        Log.i(TAG, "Installed " + out.getAbsolutePath());
        return out;
    }

    public static File installVanillaServer(CsServerConfig config, String serverJarUrl) throws Exception {
        File dir = new File(config.serverDir);
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, "server.jar");
        downloadTo(serverJarUrl, out);
        return out;
    }

    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestProperty("User-Agent", "CS-Launcher-v3");
        c.setConnectTimeout(20000);
        c.setReadTimeout(60000);
        try (InputStream in = c.getInputStream()) {
            byte[] buf = readAll(in);
            return new String(buf, StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }

    private static void downloadTo(String urlStr, File dest) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestProperty("User-Agent", "CS-Launcher-v3");
        c.setConnectTimeout(20000);
        c.setReadTimeout(120000);
        try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
        } finally {
            c.disconnect();
        }
        if (!dest.exists() || dest.length() == 0) {
            throw new IllegalStateException("Download failed: " + urlStr);
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) >= 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
