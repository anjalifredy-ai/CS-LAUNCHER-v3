package net.kdt.pojavlaunch.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads official Forge installer and runs {@code --installServer}.
 * Requires a valid MultiRT Java binary path passed in.
 */
public class CsForgeServerInstaller {

    public static class Result {
        public final boolean success;
        public final String message;
        public final String forgeVersion;

        public Result(boolean success, String message, String forgeVersion) {
            this.success = success;
            this.message = message;
            this.forgeVersion = forgeVersion;
        }
    }

    public Result install(CsServerConfig config, String javaBin) {
        if (config.softwareEnum() != CsServerSoftware.FORGE) {
            return new Result(false, "Not a Forge server", null);
        }
        if (javaBin == null || javaBin.isEmpty()) {
            return new Result(false, "Java binary required to run Forge installer", null);
        }

        File serverDir = new File(config.serverDir);
        //noinspection ResultOfMethodCallIgnored
        serverDir.mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new File(serverDir, "mods").mkdirs();

        try {
            String forgeVer = resolveLatestForge(config.minecraftVersion);
            if (forgeVer == null) {
                return new Result(false, "No Forge build for " + config.minecraftVersion, null);
            }

            String installerName = "forge-" + config.minecraftVersion + "-" + forgeVer + "-installer.jar";
            String installerUrl = "https://maven.minecraftforge.net/net/minecraftforge/forge/"
                    + config.minecraftVersion + "-" + forgeVer + "/" + installerName;

            File installerJar = new File(serverDir, installerName);
            download(installerUrl, installerJar);
            if (!installerJar.exists() || installerJar.length() == 0) {
                return new Result(false, "Forge installer download empty", null);
            }

            ProcessBuilder pb = new ProcessBuilder(
                    javaBin, "-jar", installerJar.getAbsolutePath(),
                    "--installServer", serverDir.getAbsolutePath()
            );
            pb.directory(serverDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder log = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    log.append(line).append('\n');
                    if (log.length() > 8000) break;
                }
            }
            int code = p.waitFor();
            if (code != 0) {
                String tail = log.length() > 200 ? log.substring(log.length() - 200) : log.toString();
                return new Result(false, "Forge installer exit " + code + ": " + tail.trim(), null);
            }

            ensureServerJar(serverDir, config.minecraftVersion, forgeVer);

            if (config.eulaAccepted) {
                Files.write(new File(serverDir, "eula.txt").toPath(),
                        "# CS Launcher\neula=true\n".getBytes(StandardCharsets.UTF_8));
            }
            File props = new File(serverDir, "server.properties");
            if (!props.exists()) {
                CsServerPropertiesWriter.writeDefaults(props, config);
            }

            return new Result(true, "Forge " + forgeVer + " installed", forgeVer);
        } catch (Exception e) {
            return new Result(false, e.getMessage() != null ? e.getMessage() : "Forge install failed", null);
        }
    }

    private String resolveLatestForge(String mcVersion) throws Exception {
        String metaUrl = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml";
        HttpURLConnection c = (HttpURLConnection) new URL(metaUrl).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "CS-Launcher/3");
        String xml;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            xml = sb.toString();
        }
        Pattern p = Pattern.compile("<version>" + Pattern.quote(mcVersion) + "-([^<]+)</version>");
        Matcher m = p.matcher(xml);
        String last = null;
        while (m.find()) last = m.group(1);
        return last;
    }

    private void ensureServerJar(File serverDir, String mc, String forge) throws Exception {
        File[] candidates = {
                new File(serverDir, "forge-" + mc + "-" + forge + "-shim.jar"),
                new File(serverDir, "forge-" + mc + "-" + forge + "-universal.jar"),
                new File(serverDir, "server.jar")
        };
        for (File f : candidates) {
            if (f.exists() && !f.getName().equals("server.jar")) {
                Files.copy(f.toPath(), new File(serverDir, "server.jar").toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                break;
            }
        }
        File args = new File(serverDir, "user_jvm_args.txt");
        if (!args.exists()) {
            Files.write(args.toPath(), "# CS Launcher\n".getBytes(StandardCharsets.UTF_8));
        }
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
