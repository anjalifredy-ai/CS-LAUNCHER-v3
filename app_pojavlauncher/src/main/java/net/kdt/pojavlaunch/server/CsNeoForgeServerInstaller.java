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
 * NeoForge dedicated server installer (maven.neoforged.net).
 */
public class CsNeoForgeServerInstaller {

    public static class Result {
        public final boolean success;
        public final String message;
        public final String neoVersion;

        public Result(boolean success, String message, String neoVersion) {
            this.success = success;
            this.message = message;
            this.neoVersion = neoVersion;
        }
    }

    public Result install(CsServerConfig config, String javaBin) {
        if (config.softwareEnum() != CsServerSoftware.NEOFORGE) {
            return new Result(false, "Not a NeoForge server", null);
        }
        if (javaBin == null || javaBin.isEmpty()) {
            return new Result(false, "Java binary required", null);
        }

        File serverDir = new File(config.serverDir);
        //noinspection ResultOfMethodCallIgnored
        serverDir.mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new File(serverDir, "mods").mkdirs();

        try {
            String neoVer = resolveLatest(config.minecraftVersion);
            if (neoVer == null) {
                return new Result(false, "No NeoForge for " + config.minecraftVersion, null);
            }

            // NeoForge installer naming: neoforge-{ver}-installer.jar
            String installerName = "neoforge-" + neoVer + "-installer.jar";
            String installerUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/"
                    + neoVer + "/" + installerName;

            File installerJar = new File(serverDir, installerName);
            download(installerUrl, installerJar);
            if (!installerJar.exists() || installerJar.length() == 0) {
                return new Result(false, "NeoForge installer download empty", null);
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
                return new Result(false, "NeoForge installer exit " + code + ": " + tail.trim(), null);
            }

            // Prefer run scripts / shim; copy to server.jar if present
            File[] candidates = serverDir.listFiles((dir, name) ->
                    name.startsWith("neoforge-") && name.endsWith(".jar") && !name.contains("installer"));
            if (candidates != null) {
                for (File f : candidates) {
                    if (f.getName().contains("shim") || f.getName().contains("universal")) {
                        Files.copy(f.toPath(), new File(serverDir, "server.jar").toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        break;
                    }
                }
            }

            if (config.eulaAccepted) {
                Files.write(new File(serverDir, "eula.txt").toPath(),
                        "# CS Launcher\neula=true\n".getBytes(StandardCharsets.UTF_8));
            }
            File props = new File(serverDir, "server.properties");
            if (!props.exists()) CsServerPropertiesWriter.writeDefaults(props, config);

            return new Result(true, "NeoForge " + neoVer + " installed", neoVer);
        } catch (Exception e) {
            return new Result(false, e.getMessage() != null ? e.getMessage() : "NeoForge install failed", null);
        }
    }

    private String resolveLatest(String mcVersion) throws Exception {
        // NeoForge versions often embed MC minor; metadata lists full versions
        String metaUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
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
        // Match versions that correspond to MC, e.g. 21.1.x for 1.21.1 — best effort: last version containing mc minor
        String minor = mcVersion.contains(".") ? mcVersion.substring(mcVersion.indexOf('.') + 1) : mcVersion;
        Pattern p = Pattern.compile("<version>([^<]+)</version>");
        Matcher m = p.matcher(xml);
        String last = null;
        String preferred = null;
        while (m.find()) {
            String v = m.group(1);
            last = v;
            if (v.startsWith(minor.replaceFirst("^(\\d+)\\..*", "$1") + ".") || v.contains(mcVersion)) {
                preferred = v;
            }
        }
        return preferred != null ? preferred : last;
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
