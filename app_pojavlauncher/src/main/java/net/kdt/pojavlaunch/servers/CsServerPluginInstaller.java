package net.kdt.pojavlaunch.servers;

import android.content.Context;
import android.util.Log;

import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Browse/install plugins (Paper) or mods (Fabric) via Modrinth into the server folder. */
public final class CsServerPluginInstaller {
    private static final String TAG = "CsPlugins";

    public static class Hit {
        public String projectId;
        public String title;
        public String description;
        public String slug;
    }

    private CsServerPluginInstaller() {}

    public static List<Hit> search(String query, String software, int limit) {
        List<Hit> out = new ArrayList<>();
        try {
            String soft = software != null ? software.toLowerCase() : "paper";
            String facets = "fabric".equals(soft)
                    ? "[[\"project_type:mod\"],[\"categories:fabric\"]]"
                    : "[[\"project_type:plugin\"],[\"categories:bukkit\"]]";
            String q = query != null ? query.trim() : "";
            String url = "https://api.modrinth.com/v2/search?limit=" + Math.min(limit, 20)
                    + "&query=" + java.net.URLEncoder.encode(q, "UTF-8")
                    + "&facets=" + java.net.URLEncoder.encode(facets, "UTF-8");
            String body = httpGet(url);
            if (body == null) return out;
            JSONArray hits = new JSONObject(body).optJSONArray("hits");
            if (hits == null) return out;
            for (int i = 0; i < hits.length(); i++) {
                JSONObject h = hits.getJSONObject(i);
                Hit hit = new Hit();
                hit.projectId = h.optString("project_id");
                hit.title = h.optString("title");
                hit.description = h.optString("description");
                hit.slug = h.optString("slug");
                out.add(hit);
            }
        } catch (Throwable t) {
            Log.e(TAG, "search", t);
        }
        return out;
    }

    public static File install(Context ctx, LocalServerEntry e, String projectId) throws Exception {
        File dir = LocalServerRunner.serverDir(ctx, e);
        String soft = e.software != null ? e.software.toLowerCase() : "vanilla";
        File targetDir = new File(dir, "fabric".equals(soft) ? "mods" : "plugins");
        if (!targetDir.exists() && !targetDir.mkdirs()) throw new Exception("Cannot create " + targetDir.getName());

        String body = httpGet("https://api.modrinth.com/v2/project/" + projectId + "/version");
        if (body == null) throw new Exception("Modrinth version list failed");
        JSONArray arr = new JSONArray(body);
        if (arr.length() == 0) throw new Exception("No versions");
        JSONObject chosen = arr.getJSONObject(0);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject v = arr.getJSONObject(i);
            JSONArray game = v.optJSONArray("game_versions");
            if (game != null && e.versionId != null) {
                for (int g = 0; g < game.length(); g++) {
                    if (e.versionId.equals(game.optString(g))) {
                        chosen = v;
                        break;
                    }
                }
            }
        }
        JSONArray files = chosen.getJSONArray("files");
        JSONObject primary = files.getJSONObject(0);
        for (int i = 0; i < files.length(); i++) {
            if (files.getJSONObject(i).optBoolean("primary")) primary = files.getJSONObject(i);
        }
        String dl = primary.getString("url");
        String fname = primary.optString("filename", projectId + ".jar");
        File dest = new File(targetDir, fname);
        DownloadUtils.downloadFile(dl, dest);
        return dest;
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
