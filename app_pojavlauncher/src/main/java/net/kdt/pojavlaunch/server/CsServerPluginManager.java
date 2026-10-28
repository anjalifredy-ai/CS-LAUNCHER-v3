package net.kdt.pojavlaunch.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CsServerPluginManager {

    public List<String> list(CsServerConfig config) {
        File plugins = new File(config.serverDir, "plugins");
        List<String> names = new ArrayList<>();
        if (!plugins.isDirectory()) return names;
        File[] files = plugins.listFiles((dir, name) -> name.endsWith(".jar") || name.endsWith(".jar.disabled"));
        if (files != null) {
            for (File f : files) names.add(f.getName());
        }
        return names;
    }

    public File installFromUrl(CsServerConfig config, String url, String fileName) throws Exception {
        File plugins = new File(config.serverDir, "plugins");
        if (!plugins.exists()) plugins.mkdirs();
        File dest = new File(plugins, fileName);
        downloadTo(url, dest);
        return dest;
    }

    public boolean setEnabled(CsServerConfig config, String fileName, boolean enabled) {
        File plugins = new File(config.serverDir, "plugins");
        File current = new File(plugins, fileName);
        if (!current.exists()) {
            // try alternate
            if (enabled) {
                current = new File(plugins, fileName.endsWith(".disabled") ? fileName : fileName + ".disabled");
            } else {
                current = new File(plugins, fileName.endsWith(".jar") ? fileName : fileName.replace(".disabled", ""));
            }
        }
        if (!current.exists()) return false;
        if (enabled && current.getName().endsWith(".disabled")) {
            String newName = current.getName().replace(".disabled", "");
            return current.renameTo(new File(plugins, newName));
        }
        if (!enabled && !current.getName().endsWith(".disabled")) {
            return current.renameTo(new File(plugins, current.getName() + ".disabled"));
        }
        return true;
    }

    public boolean delete(CsServerConfig config, String fileName) {
        File f = new File(new File(config.serverDir, "plugins"), fileName);
        return f.exists() && f.delete();
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
