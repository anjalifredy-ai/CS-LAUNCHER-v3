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
 * Bedrock version registry + isolation + official APK import (Day 1–2).
 * Full multi-version play host still later (Day 31+).
 */
public final class BedrockVersionManager {
    private static final String PREF = "remom_bedrock_versions";
    private static final String KEY_REG = "registry";
    public static final String APK_FILE_NAME = "official.apk";

    public interface CopyProgress {
        void onProgress(long copied, long totalOrMinus1);
        void onDone(boolean success, File apkFile, String error);
    }

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

    public static File apkFile(Context c, String versionId) {
        return new File(new File(isolationDir(c, versionId), "meta"), APK_FILE_NAME);
    }

    public static boolean hasFullApk(Context c, String versionId) {
        File f = apkFile(c, versionId);
        return f.isFile() && f.length() > 1024 * 100; // >100KB sanity
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
        out.add(new VersionEntry("Play Store (current)", "playstore", true, hasFullApk(c, "playstore")));
        try {
            String raw = p(c).getString(KEY_REG, "[]");
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String id = o.getString("id");
                out.add(new VersionEntry(
                        o.optString("label", id),
                        id,
                        o.optBoolean("isolationReady", false),
                        hasFullApk(c, id) || o.optBoolean("apkImported", false)
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

    /** Day 1: URI pointer only. */
    public static boolean importOfficialApkMeta(Context c, String versionId, Uri apkUri) {
        if (c == null || apkUri == null || versionId == null) return false;
        try {
            File dir = isolationDir(c, versionId);
            File dest = new File(new File(dir, "meta"), "official.apk.ref");
            try (FileOutputStream out = new FileOutputStream(dest)) {
                out.write(apkUri.toString().getBytes("UTF-8"));
            }
            markImported(c, versionId, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Day 2: full APK bytes → isolation meta/official.apk.
     * Call from background thread; progress callbacks may be any thread.
     */
    public static void copyOfficialApk(Context c, String versionId, Uri apkUri, CopyProgress cb) {
        if (c == null || apkUri == null || versionId == null) {
            if (cb != null) cb.onDone(false, null, "null args");
            return;
        }
        File dest = apkFile(c, versionId);
        File tmp = new File(dest.getParentFile(), APK_FILE_NAME + ".part");
        try {
            //noinspection ResultOfMethodCallIgnored
            dest.getParentFile().mkdirs();
            long total = -1;
            try {
                android.database.Cursor cursor = c.getContentResolver().query(apkUri, null, null, null, null);
                if (cursor != null) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (idx >= 0 && cursor.moveToFirst()) total = cursor.getLong(idx);
                    cursor.close();
                }
            } catch (Exception ignored) {}

            try (InputStream in = c.getContentResolver().openInputStream(apkUri);
                 FileOutputStream out = new FileOutputStream(tmp)) {
                if (in == null) {
                    if (cb != null) cb.onDone(false, null, "cannot open stream");
                    return;
                }
                byte[] buf = new byte[64 * 1024];
                long copied = 0;
                int n;
                while ((n = in.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                    copied += n;
                    if (cb != null) cb.onProgress(copied, total);
                }
                out.flush();
            }
            if (dest.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dest.delete();
            }
            if (!tmp.renameTo(dest)) {
                // fallback copy rename fail
                try (InputStream in2 = new java.io.FileInputStream(tmp);
                     FileOutputStream out2 = new FileOutputStream(dest)) {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in2.read(buf)) >= 0) out2.write(buf, 0, n);
                }
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
            markImported(c, versionId, true);
            importOfficialApkMeta(c, versionId, apkUri);
            if (cb != null) cb.onDone(true, dest, null);
        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            if (cb != null) cb.onDone(false, null, e.getMessage());
        }
    }

    private static void markImported(Context c, String versionId, boolean full) {
        try {
            JSONArray arr = new JSONArray(p(c).getString(KEY_REG, "[]"));
            boolean found = false;
            for (int i = 0; i < arr.length(); i++) {
                if (versionId.equals(arr.getJSONObject(i).optString("id"))) {
                    arr.getJSONObject(i).put("apkImported", true);
                    arr.getJSONObject(i).put("isolationReady", true);
                    arr.getJSONObject(i).put("fullApk", full);
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
                o.put("fullApk", full);
                arr.put(o);
            }
            p(c).edit().putString(KEY_REG, arr.toString()).apply();
        } catch (Exception ignored) {}
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
