package net.kdt.pojavlaunch.edition;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3 foundation — multi-version Bedrock management.
 *
 * What this does NOW:
 * - Registry of version ids + isolation folders under app files
 * - Prepare worlds/resource_packs/behavior_packs dirs per version
 * - Import metadata from a user-picked APK Uri (must be treated as OWNED official only)
 *
 * What this does NOT do yet:
 * - Load Mojang native libs for arbitrary old versions inside Remom process
 * - Download cracked/unofficial Bedrock clients
 * - Bypass Play license
 *
 * Real multi-version native host ≈ LeviLaunchroid-class project (ongoing).
 * Until that host exists, {@link EditionMode#tryLaunchBedrock} still opens
 * the single official Play package; isolation dirs are ready for data separation.
 */
public final class BedrockVersionManager {
    private static final String PREF = "remom_bedrock_versions";
    private static final String KEY_REG = "registry";

    private BedrockVersionManager() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static File rootDir(Context c) {
        File f = new File(c.getFilesDir(), "bedrock_versions");
        //noinspection ResultOfMethodCallIgnored
        f.mkdirs();
        return f;
    }

    public static File isolationDir(Context c, String versionId) {
        String safe = versionId == null ? "default" : versionId.replaceAll("[^a-zA-Z0-9._-]", "_");
        File f = new File(rootDir(c), safe);
        ensureTree(f);
        return f;
    }

    private static void ensureTree(File base) {
        //noinspection ResultOfMethodCallIgnored
        base.mkdirs();
        for (String sub : new String[]{"games/com.mojang/minecraftWorlds",
                "games/com.mojang/resource_packs",
                "games/com.mojang/behavior_packs",
                "meta"}) {
            //noinspection ResultOfMethodCallIgnored
            new File(base, sub).mkdirs();
        }
    }

    public static List<VersionEntry> list(Context c) {
        List<VersionEntry> out = new ArrayList<>();
        // Always include play-store current
        out.add(new VersionEntry("Play Store (current)", "playstore", true, false));
        try {
            String raw = p(c).getString(KEY_REG, "[]");
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new VersionEntry(
                        o.optString("label", o.getString("id")),
                        o.getString("id"),
                        o.optBoolean("isolationReady", false),
                        o.optBoolean("apkImported", false)
                ));
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static void registerVersion(Context c, String id, String label) {
        try {
            JSONArray arr = new JSONArray(p(c).getString(KEY_REG, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                if (id.equals(arr.getJSONObject(i).optString("id"))) {
                    isolationDir(c, id);
                    arr.getJSONObject(i).put("isolationReady", true);
                    p(c).edit().putString(KEY_REG, arr.toString()).apply();
                    return;
                }
            }
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("label", label);
            o.put("isolationReady", true);
            o.put("apkImported", false);
            arr.put(o);
            isolationDir(c, id);
            p(c).edit().putString(KEY_REG, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    /**
     * Copy a user-selected APK into meta/ for bookkeeping.
     * Does NOT install or execute natives. Caller must only offer this for
     * APKs the user is allowed to keep (their owned official builds).
     */
    public static boolean importOfficialApkMeta(Context c, String versionId, Uri apkUri) {
        if (c == null || apkUri == null || versionId == null) return false;
        try {
            File dir = isolationDir(c, versionId);
            File dest = new File(new File(dir, "meta"), "official.apk.ref");
            try (InputStream in = c.getContentResolver().openInputStream(apkUri);
                 FileOutputStream out = new FileOutputStream(dest)) {
                if (in == null) return false;
                byte[] buf = new byte[8192];
                int n;
                long total = 0;
                // Cap meta copy note file — store only first 0 bytes marker + uri string for large apks
                // Full APK copy can be huge; write pointer file instead if size unknown
                String pointer = apkUri.toString();
                out.write(pointer.getBytes("UTF-8"));
            }
            // Mark registry
            JSONArray arr = new JSONArray(p(c).getString(KEY_REG, "[]"));
            boolean found = false;
            for (int i = 0; i < arr.length(); i++) {
                if (versionId.equals(arr.getJSONObject(i).optString("id"))) {
                    arr.getJSONObject(i).put("apkImported", true);
                    arr.getJSONObject(i).put("isolationReady", true);
                    found = true;
                    break;
                }
            }
            if (!found) {
                JSONObject o = new JSONObject();
                o.put("id", versionId);
                o.put("label", versionId);
                o.put("isolationReady", true);
                o.put("apkImported", true);
                arr.put(o);
            }
            p(c).edit().putString(KEY_REG, arr.toString()).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static final class VersionEntry {
        public final String label;
        public final String id;
        public final boolean isolationReady;
        public final boolean apkImported;

        public VersionEntry(String label, String id, boolean isolationReady, boolean apkImported) {
            this.label = label;
            this.id = id;
            this.isolationReady = isolationReady;
            this.apkImported = apkImported;
        }
    }
}
