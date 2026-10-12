package net.kdt.pojavlaunch.edition;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Simple JSON store for Bedrock profiles (Phase 2 UI). */
public final class BedrockProfileStore {
    private static final String PREF = "remom_bedrock_profiles";
    private static final String KEY_LIST = "list";
    private static final String KEY_SELECTED = "selected";

    private BedrockProfileStore() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static List<BedrockProfile> list(Context c) {
        List<BedrockProfile> out = new ArrayList<>();
        try {
            String raw = p(c).getString(KEY_LIST, "[]");
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new BedrockProfile(
                        o.getString("id"),
                        o.getString("name"),
                        o.optString("versionId", null),
                        o.optString("isolationDir", null)
                ));
            }
        } catch (Exception ignored) {}
        if (out.isEmpty()) {
            BedrockProfile def = BedrockProfile.playStoreDefault();
            out.add(def);
            saveAll(c, out);
            setSelectedId(c, def.id);
        }
        return out;
    }

    public static void saveAll(Context c, List<BedrockProfile> list) {
        try {
            JSONArray arr = new JSONArray();
            for (BedrockProfile bp : list) {
                JSONObject o = new JSONObject();
                o.put("id", bp.id);
                o.put("name", bp.name);
                o.put("versionId", bp.versionId != null ? bp.versionId : JSONObject.NULL);
                o.put("isolationDir", bp.isolationDir != null ? bp.isolationDir : JSONObject.NULL);
                arr.put(o);
            }
            p(c).edit().putString(KEY_LIST, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static BedrockProfile add(Context c, String name, String versionId) {
        List<BedrockProfile> list = list(c);
        BedrockProfile bp = new BedrockProfile(
                UUID.randomUUID().toString(),
                name == null || name.trim().isEmpty() ? "Bedrock profile" : name.trim(),
                versionId,
                null
        );
        list.add(bp);
        saveAll(c, list);
        setSelectedId(c, bp.id);
        return bp;
    }

    public static void setSelectedId(Context c, String id) {
        p(c).edit().putString(KEY_SELECTED, id).apply();
    }

    public static String getSelectedId(Context c) {
        return p(c).getString(KEY_SELECTED, "playstore_default");
    }

    public static BedrockProfile getSelected(Context c) {
        String id = getSelectedId(c);
        for (BedrockProfile bp : list(c)) {
            if (bp.id.equals(id)) return bp;
        }
        return BedrockProfile.playStoreDefault();
    }

    public static void setVersionForSelected(Context c, String versionId) {
        List<BedrockProfile> list = list(c);
        String id = getSelectedId(c);
        List<BedrockProfile> next = new ArrayList<>();
        for (BedrockProfile bp : list) {
            if (bp.id.equals(id)) {
                next.add(new BedrockProfile(bp.id, bp.name, versionId, bp.isolationDir));
            } else {
                next.add(bp);
            }
        }
        saveAll(c, next);
    }

    /** UI catalog — not downloadable engines until Phase 3. */
    public static String[] catalogVersions() {
        return new String[]{
                "Play Store (current)",
                "1.21.90",
                "1.21.80",
                "1.21.70",
                "1.21.50",
                "1.21.30",
                "1.20.80",
                "1.20.50",
                "1.19.80"
        };
    }
}
