package net.kdt.pojavlaunch.cosmetics;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/**
 * Downloads and caches cosmetic assets by URL/hash under cs_cosmetics/cache.
 */
public class CsCosmeticAssetCache {

    private final File cacheDir;

    public CsCosmeticAssetCache() {
        cacheDir = new File(Tools.DIR_GAME_HOME, "cs_cosmetics/cache");
        if (!cacheDir.exists()) cacheDir.mkdirs();
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public File resolveOrDownload(String url, String preferredName) throws Exception {
        String key = preferredName != null ? preferredName : sha1(url).substring(0, 16);
        File dest = new File(cacheDir, key);
        if (dest.exists() && dest.length() > 0) return dest;
        downloadTo(url, dest);
        return dest;
    }

    public void clear() {
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File f : files) //noinspection ResultOfMethodCallIgnored
                f.delete();
        }
    }

    private void downloadTo(String urlStr, File dest) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
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

    private static String sha1(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] dig = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
