package net.kdt.pojavlaunch.server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Quilt dedicated server via meta.quiltmc.org server launcher jar.
 */
public class CsQuiltServerInstaller {

    public static class Result {
        public final boolean success;
        public final String message;
        public final File serverJar;
        public final String loaderVersion;

        public Result(boolean success, String message, File serverJar, String loaderVersion) {
            this.success = success;
            this.message = message;
            this.serverJar = serverJar;
            this.loaderVersion = loaderVersion;
        }
    }

    public Result install(CsServerConfig config) {
        if (config.softwareEnum() != CsServerSoftware.QUILT) {
            return new Result(false, "Not a Quilt server", null, null);
        }

        File serverDir = new File(config.serverDir);
        //noinspection ResultOfMethodCallIgnored
        serverDir.mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new File(serverDir, "mods").mkdirs();

        try {
            String loader = latestLoader(config.minecraftVersion);
            if (loader == null) {
                return new Result(false, "No Quilt loader for " + config.minecraftVersion, null, null);
            }

            // Quilt server jar endpoint similar to Fabric meta
            String jarUrl = "https://meta.quiltmc.org/v3/versions/loader/"
                    + config.minecraftVersion + "/" + loader + "/server/jar";

            File jar = new File(serverDir, "server.jar");
            download(jarUrl, jar);
            if (!jar.exists() || jar.length() == 0) {
                // Fallback: installer style not always available — report clearly
                return new Result(false, "Quilt server jar download failed (check MC version support)", null, loader);
            }

            if (config.eulaAccepted) {
                Files.write(new File(serverDir, "eula.txt").toPath(),
                        "# CS Launcher\neula=true\n".getBytes(StandardCharsets.UTF_8));
            }
            File props = new File(serverDir, "server.properties");
            if (!props.exists()) CsServerPropertiesWriter.writeDefaults(props, config);

            return new Result(true, "Quilt loader " + loader + " installed", jar, loader);
        } catch (Exception e) {
            return new Result(false, e.getMessage() != null ? e.getMessage() : "Quilt install failed", null, null);
        }
    }

    private String latestLoader(String mcVersion) throws Exception {
        String url = "https://meta.quiltmc.org/v3/versions/loader/" + mcVersion;
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", "CS-Launcher/3");
        String body;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            body = sb.toString();
        }
        JSONArray arr = new JSONArray(body);
        if (arr.length() == 0) return null;
        JSONObject first = arr.getJSONObject(0);
        if (first.has("loader")) {
            return first.getJSONObject("loader").optString("version", null);
        }
        return first.optString("version", null);
    }

    private void download(String urlStr, File dest) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setConnectTimeout(30000);
        c.setReadTimeout(120000);
        c.setRequestProperty("User-Agent", "CS-Launcher/3");
        c.setInstanceFollowRedirects(true);
        try (java.io.InputStream in = c.getInputStream();
             java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
        }
    }
}
