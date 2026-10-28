package net.kdt.pojavlaunch.voice;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Installs Simple Voice Chat (Modrinth project) into a profile mods folder.
 * Supports Fabric / Forge / Quilt / NeoForge loaders via Modrinth version filters.
 * Requires RECORD_AUDIO at runtime when the mod is used in-game.
 */
public class CsVoiceChatInstaller {

    public static final String MODRINTH_PROJECT = "simple-voice-chat";

    public static class Result {
        public final boolean success;
        public final String message;
        public final File modFile;

        public Result(boolean success, String message, File modFile) {
            this.success = success;
            this.message = message;
            this.modFile = modFile;
        }
    }

    /**
     * @param modsDir profile mods directory
     * @param minecraftVersion e.g. 1.21.1
     * @param loader fabric | forge | quilt | neoforge
     */
    public Result install(File modsDir, String minecraftVersion, String loader) {
        if (modsDir == null) {
            return new Result(false, "mods dir is null", null);
        }
        //noinspection ResultOfMethodCallIgnored
        modsDir.mkdirs();

        try {
            String api = "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT
                    + "/version?game_versions=[%22" + minecraftVersion + "%22]&loaders=[%22"
                    + loader.toLowerCase() + "%22]";

            HttpURLConnection c = (HttpURLConnection) new URL(api).openConnection();
            c.setConnectTimeout(20000);
            c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent", "CS-Launcher/3");
            String body;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                body = sb.toString();
            }

            JSONArray versions = new JSONArray(body);
            if (versions.length() == 0) {
                return new Result(false,
                        "No Simple Voice Chat build for " + minecraftVersion + " / " + loader, null);
            }

            JSONObject ver = versions.getJSONObject(0);
            JSONArray files = ver.getJSONArray("files");
            if (files.length() == 0) {
                return new Result(false, "Version has no files", null);
            }

            JSONObject primary = files.getJSONObject(0);
            for (int i = 0; i < files.length(); i++) {
                JSONObject f = files.getJSONObject(i);
                if (f.optBoolean("primary", false)) {
                    primary = f;
                    break;
                }
            }

            String url = primary.getString("url");
            String filename = primary.getString("filename");
            File dest = new File(modsDir, filename);
            download(url, dest);

            if (!dest.exists() || dest.length() == 0) {
                return new Result(false, "Download empty", null);
            }

            return new Result(true,
                    "Simple Voice Chat installed: " + filename
                            + " (grant RECORD_AUDIO when launching)", dest);
        } catch (Exception e) {
            return new Result(false, e.getMessage() != null ? e.getMessage() : "Voice chat install failed", null);
        }
    }

    public boolean isInstalled(File modsDir) {
        if (modsDir == null || !modsDir.isDirectory()) return false;
        File[] files = modsDir.listFiles();
        if (files == null) return false;
        for (File f : files) {
            String n = f.getName().toLowerCase();
            if (n.contains("voice-chat") || n.contains("voicechat") || n.contains("simple-voice")) {
                return true;
            }
        }
        return false;
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
