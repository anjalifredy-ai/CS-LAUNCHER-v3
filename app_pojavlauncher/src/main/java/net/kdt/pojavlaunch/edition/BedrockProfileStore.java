package net.kdt.pojavlaunch.edition;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bedrock profiles + Day 5 APK/isolation bind. */
public final class BedrockProfileStore {
    private static final String PREF = "remom_bedrock_profiles";
    private static final String KEY_LIST = "list";
    private static final String KEY_SELECTED = "selected";

    private BedrockProfileStore() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static String optNull(JSONObject o, String key) {
        if (!o.has(key) || o.isNull(key)) return null;
        String s = o.optString(key, "");
        return s.isEmpty() ? null : s;
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
                        optNull(o, "versionId"),
                        optNull(o, "isolationDir"),
                        optNull(o, "apkPath")
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
                o.put("apkPath", bp.apkPath != null ? bp.apkPath : JSONObject.NULL);
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
                null,
                null
        );
        list.add(bp);
        saveAll(c, list);
        setSelectedId(c, bp.id);
        // Bind paths for default version slot
        bindSelectedToVersion(c, versionId != null ? versionId : "Play Store (current)");
        return getSelected(c);
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
        bindSelectedToVersion(c, versionId);
    }

    /**
     * Day 5: bind selected profile to version slot + isolation dir + apk path if imported.
     */
    public static void bindSelectedToVersion(Context c, String versionLabelOrId) {
        if (c == null) return;
        String slot = versionLabelOrId == null || versionLabelOrId.startsWith("Play Store")
                ? "playstore"
                : versionLabelOrId;
        String label = versionLabelOrId != null ? versionLabelOrId : "Play Store (current)";

        BedrockVersionManager.registerVersion(c, slot, label);
        File iso = BedrockVersionManager.isolationDir(c, slot);
        File apk = BedrockVersionManager.apkFile(c, slot);
        String apkPath = (apk.isFile() && apk.length() > 1024 * 100) ? apk.getAbsolutePath() : null;

        String id = getSelectedId(c);
        List<BedrockProfile> next = new ArrayList<>();
        for (BedrockProfile bp : list(c)) {
            if (bp.id.equals(id)) {
                next.add(new BedrockProfile(
                        bp.id,
                        bp.name,
                        label,
                        iso.getAbsolutePath(),
                        apkPath
                ));
            } else {
                next.add(bp);
            }
        }
        saveAll(c, next);
    }

    /** One-line status for profile list UI. */
    public static String bindingSummary(Context c, BedrockProfile bp) {
        if (bp == null) return "—";
        String slot = bp.versionId == null || bp.versionId.startsWith("Play Store")
                ? "playstore" : bp.versionId;
        String detected = BedrockVersionManager.getDetectedVersionName(c, slot);
        boolean hasApk = bp.apkPath != null
                || BedrockVersionManager.hasFullApk(c, slot);
        if (hasApk) {
            return "Bound · Imported" + (detected != null ? " v" + detected : "")
                    + (bp.apkPath != null ? "\n    " + shortPath(bp.apkPath) : "");
        }
        if (EditionMode.isOfficialBedrockInstalled(c) && "playstore".equals(slot)) {
            return "Bound · Play Store (no local APK copy)";
        }
        return "Bound · Missing APK — import needed";
    }

    private static String shortPath(String path) {
        if (path == null) return "";
        int i = path.lastIndexOf('/');
        if (i > 0 && i < path.length() - 1) {
            // show …/versionId/meta/official.apk
            int j = path.lastIndexOf('/', i - 1);
            if (j > 0) return "…" + path.substring(j);
        }
        return path;
    }

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
