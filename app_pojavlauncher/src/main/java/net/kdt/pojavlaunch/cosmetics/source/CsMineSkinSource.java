package net.kdt.pojavlaunch.cosmetics.source;

import net.kdt.pojavlaunch.cosmetics.CsCosmeticCategory;
import net.kdt.pojavlaunch.cosmetics.CsCosmeticCollection;
import net.kdt.pojavlaunch.cosmetics.CsCosmeticItem;
import net.kdt.pojavlaunch.cosmetics.CsCosmeticRarity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches public skins from MineSkin API gallery (no API key for public list).
 * Pagination supported.
 */
public class CsMineSkinSource implements CsCosmeticSource {

    private static final String API = "https://api.mineskin.org/get/list";

    @Override
    public String getId() { return "mineskin"; }

    @Override
    public String getDisplayName() { return "MineSkin Gallery"; }

    @Override
    public boolean isAvailable() { return true; }

    @Override
    public List<CsCosmeticItem> fetchPage(String query, int page, int pageSize) throws Exception {
        int size = Math.min(Math.max(pageSize, 1), 32);
        // MineSkin list uses page param starting at 1
        String url = API + "?page=" + (page + 1) + "&size=" + size;
        String json = httpGet(url);
        JSONObject root = new JSONObject(json);
        JSONArray skins = root.optJSONArray("skins");
        List<CsCosmeticItem> out = new ArrayList<>();
        if (skins == null) return out;

        String q = query != null ? query.trim().toLowerCase() : "";
        for (int i = 0; i < skins.length(); i++) {
            JSONObject s = skins.getJSONObject(i);
            String name = s.optString("name", "Skin");
            if (!q.isEmpty() && !name.toLowerCase().contains(q)) continue;

            String uuid = s.optString("uuid", String.valueOf(s.optLong("id")));
            String textureUrl = null;
            JSONObject data = s.optJSONObject("data");
            if (data != null) {
                JSONObject texture = data.optJSONObject("texture");
                if (texture != null) textureUrl = texture.optString("url", null);
            }
            if (textureUrl == null) {
                // fallback common pattern
                textureUrl = "https://textures.minecraft.net/texture/" + s.optString("texture", "");
            }

            CsCosmeticItem item = new CsCosmeticItem();
            item.id = "mineskin_" + uuid;
            item.name = name.isEmpty() ? "MineSkin " + uuid.substring(0, Math.min(8, uuid.length())) : name;
            item.description = "MineSkin gallery skin";
            item.category = CsCosmeticCategory.SKINS;
            item.rarity = CsCosmeticRarity.COMMON;
            item.collection = CsCosmeticCollection.COMMUNITY;
            item.free = true;
            item.assetPath = textureUrl;
            item.thumbnailPath = textureUrl;
            item.skinModel = s.optString("variant", "classic");
            out.add(item);
        }
        return out;
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(20000);
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
}
