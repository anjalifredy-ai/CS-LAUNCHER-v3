package net.kdt.pojavlaunch.cosmetics;

import android.content.Context;
import android.content.SharedPreferences;

import net.kdt.pojavlaunch.Tools;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Local wardrobe + favorites for CS Launcher (ported from PRO concepts).
 * Skins saved under game dir; no multiplayer claim.
 */
public class CsCosmeticsStore {

    private final SharedPreferences prefs;
    private final File skinsDir;

    public CsCosmeticsStore(Context context) {
        prefs = context.getSharedPreferences("cs_cosmetics", Context.MODE_PRIVATE);
        skinsDir = new File(Tools.DIR_GAME_HOME, "cs_cosmetics/skins");
        if (!skinsDir.exists()) skinsDir.mkdirs();
    }

    public Set<String> getFavorites() {
        return new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));
    }

    public void toggleFavorite(String id) {
        Set<String> set = getFavorites();
        if (!set.add(id)) set.remove(id);
        prefs.edit().putStringSet("favorites", set).apply();
    }

    public String getEquippedSkinId() {
        return prefs.getString("skin_id", null);
    }

    public void setEquippedSkinId(String id) {
        prefs.edit().putString("skin_id", id).apply();
    }

    public File getSkinsDir() {
        return skinsDir;
    }

    /** Copy validated PNG into skins dir and equip. */
    public File importSkin(File sourcePng, String displayName) throws Exception {
        if (!sourcePng.exists()) throw new IllegalArgumentException("Missing file");
        if (sourcePng.length() > 2 * 1024 * 1024) throw new IllegalArgumentException("Max 2MB");
        String id = "imported_" + UUID.randomUUID().toString().substring(0, 8);
        File dest = new File(skinsDir, id + ".png");
        copy(sourcePng, dest);
        // also mirror for local profile
        File profileSkin = new File(Tools.DIR_GAME_HOME, "skin.png");
        copy(dest, profileSkin);
        setEquippedSkinId(id);
        rememberImported(id, displayName != null ? displayName : sourcePng.getName(), dest.getAbsolutePath());
        return dest;
    }

    public List<String> listImportedIds() {
        List<String> ids = new ArrayList<>();
        try {
            String raw = prefs.getString("imported", "[]");
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                ids.add(arr.getJSONObject(i).getString("id"));
            }
        } catch (Exception ignored) {}
        return ids;
    }

    private void rememberImported(String id, String name, String path) {
        try {
            String raw = prefs.getString("imported", "[]");
            JSONArray arr = new JSONArray(raw);
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("name", name);
            o.put("path", path);
            arr.put(o);
            prefs.edit().putString("imported", arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void copy(File src, File dst) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
        }
    }
}
