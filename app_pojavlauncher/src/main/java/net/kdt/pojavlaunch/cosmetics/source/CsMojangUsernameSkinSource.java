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
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Resolves a Mojang username → current skin texture URL (public profile API).
 * One-item "page" when query is a valid username.
 */
public class CsMojangUsernameSkinSource implements CsCosmeticSource {

    private static final String PROFILE = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION = "https://sessionserver.mojang.com/session/minecraft/profile/";

    @Override
    public String getId() { return "mojang_username"; }

    @Override
    public String getDisplayName() { return "Mojang Username"; }

    @Override
    public boolean isAvailable() { return true; }

    @Override
    public List<CsCosmeticItem> fetchPage(String query, int page, int pageSize) throws Exception {
        if (query == null || query.trim().isEmpty() || page > 0) {
            return Collections.emptyList();
        }
        String username = query.trim();
        String uuidJson = httpGet(PROFILE + username);
        if (uuidJson == null || uuidJson.isEmpty()) return Collections.emptyList();
        JSONObject uuidObj = new JSONObject(uuidJson);
        String uuid = uuidObj.optString("id", null);
        if (uuid == null) return Collections.emptyList();

        String profileJson = httpGet(SESSION + uuid);
        JSONObject profile = new JSONObject(profileJson);
        JSONArray props = profile.optJSONArray("properties");
        if (props == null) return Collections.emptyList();

        String textureUrl = null;
        String model = "classic";
        for (int i = 0; i < props.length(); i++) {
            JSONObject p = props.getJSONObject(i);
            if (!"textures".equals(p.optString("name"))) continue;
            String b64 = p.getString("value");
            String decoded = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
            JSONObject texRoot = new JSONObject(decoded);
            JSONObject textures = texRoot.optJSONObject("textures");
            if (textures == null) continue;
            JSONObject skin = textures.optJSONObject("SKIN");
            if (skin == null) continue;
            textureUrl = skin.optString("url", null);
            JSONObject meta = skin.optJSONObject("metadata");
            if (meta != null && "slim".equals(meta.optString("model"))) {
                model = "slim";
            }
        }
        if (textureUrl == null) return Collections.emptyList();

        CsCosmeticItem item = new CsCosmeticItem();
        item.id = "mojang_" + uuid;
        item.name = username;
        item.description = "Official skin for " + username;
        item.category = CsCosmeticCategory.SKINS;
        item.rarity = CsCosmeticRarity.COMMON;
        item.collection = CsCosmeticCollection.COMMUNITY;
        item.free = true;
        item.assetPath = textureUrl;
        item.thumbnailPath = textureUrl;
        item.skinModel = model;

        List<CsCosmeticItem> list = new ArrayList<>();
        list.add(item);
        return list;
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "CS-Launcher/3.0");
        int code = conn.getResponseCode();
        if (code == 204 || code == 404) return null;
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
