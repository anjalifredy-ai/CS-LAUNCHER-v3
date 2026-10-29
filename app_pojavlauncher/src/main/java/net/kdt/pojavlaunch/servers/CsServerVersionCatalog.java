package net.kdt.pojavlaunch.servers;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Populates Minecraft Java release versions from Mojang version manifest (not free-typed).
 */
public final class CsServerVersionCatalog {
    private static final String TAG = "CsServerVersions";
    private static final String MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    private CsServerVersionCatalog() {}

    public static List<String> fetchReleaseIds(int max) {
        List<String> out = new ArrayList<>();
        try {
            String body = httpGet(MANIFEST);
            if (body == null) return fallback();
            JSONObject root = new JSONObject(body);
            JSONArray versions = root.getJSONArray("versions");
            for (int i = 0; i < versions.length() && out.size() < max; i++) {
                JSONObject v = versions.getJSONObject(i);
                if ("release".equals(v.optString("type"))) {
                    String id = v.optString("id");
                    if (id != null && !id.isEmpty()) out.add(id);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "manifest failed", t);
            return fallback();
        }
        return out.isEmpty() ? fallback() : out;
    }

    private static List<String> fallback() {
        List<String> f = new ArrayList<>();
        f.add("1.21.1");
        f.add("1.21");
        f.add("1.20.4");
        f.add("1.20.1");
        f.add("1.19.4");
        f.add("1.18.2");
        f.add("1.16.5");
        return f;
    }

    public static String[] softwareChoices() {
        return new String[]{"vanilla", "paper", "purpur", "fabric"};
    }

    public static String softwareHint(String soft) {
        if (soft == null) return "";
        switch (soft.toLowerCase()) {
            case "vanilla": return "Official Mojang server.jar";
            case "paper": return "Paper (plugins) — download from Paper API";
            case "purpur": return "Purpur — download from Purpur API";
            case "fabric": return "Fabric server loader jar";
            case "neoforge": return "Not enabled on Android yet — installer not verified";
            case "forge": return "Not enabled on Android yet — installer not verified";
            default: return "";
        }
    }

    private static String httpGet(String urlStr) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent", "CS-Launcher/3");
            try (InputStream in = c.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Throwable t) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
