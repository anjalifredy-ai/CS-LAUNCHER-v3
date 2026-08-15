package net.kdt.pojavlaunch.modloaders.modpacks;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.io.File;

/**
 * Tracks which store projects (mods / packs / shaders) are installed into each
 * profile, so the browse cards can render real states:
 *   NONE              → show the download button
 *   INSTALLED         → green ✓ "Installed in this Profile" (download hidden)
 *   UPDATE_AVAILABLE  → amber pill, download stays available
 *   INSTALLED_NEWER   → green pill, installed version is newer than store latest
 *
 * Data source of truth: entries are written by ModInstallFragment when a
 * download into a profile completes, and the "latest known store version" is
 * recorded by ModVersionPickerFragment whenever a version list is fetched.
 * The jar file itself is re-checked on query, so manual deletions from the
 * Manage Mods screen auto-heal the index.
 */
public final class InstalledContentTracker {

    public static final int STATE_NONE = 0;
    public static final int STATE_INSTALLED = 1;
    public static final int STATE_UPDATE_AVAILABLE = 2;
    public static final int STATE_INSTALLED_NEWER = 3;

    private static final String PREFS = "installed_content_index";

    private InstalledContentTracker() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(@Nullable String profileKey, String contentType, String modId) {
        return (profileKey == null ? "default" : profileKey) + "|" + contentType + "|" + modId;
    }

    private static String latestKey(String contentType, String modId) {
        return "latest|" + contentType + "|" + modId;
    }

    /** Called when a store download into a profile finishes successfully. */
    public static void markInstalled(Context ctx, @Nullable String profileKey,
                                     String contentType, String modId,
                                     @Nullable String versionName, @Nullable String fileName) {
        if (modId == null) return;
        prefs(ctx).edit()
                .putString(key(profileKey, contentType, modId),
                        (versionName == null ? "" : versionName) + "|"
                                + (fileName == null ? "" : fileName) + "|"
                                + System.currentTimeMillis())
                .apply();
    }

    /** Called when a versions list is fetched for a project (latest = first sorted entry). */
    public static void recordLatestKnown(Context ctx, String contentType,
                                         String modId, @Nullable String versionName) {
        if (modId == null || versionName == null || versionName.isEmpty()) return;
        prefs(ctx).edit().putString(latestKey(contentType, modId), versionName).apply();
    }

    /**
     * Resolve the card state for a project inside a profile.
     *
     * @param contentDir directory where this content type lives for the profile
     *                   (mods / resourcepacks / shaderpacks / saves); used to
     *                   verify the installed file still exists. May be null.
     */
    public static final class InstallInfo {
        public final int state;
        @Nullable public final String version;
        public final long installedAt;

        InstallInfo(int state, @Nullable String version, long installedAt) {
            this.state = state;
            this.version = version;
            this.installedAt = installedAt;
        }
    }

    public static int queryState(Context ctx, @Nullable String profileKey,
                                 String contentType, String modId,
                                 @Nullable File contentDir) {
        return queryInfo(ctx, profileKey, contentType, modId, contentDir).state;
    }

    /** State plus the version/date metadata rendered by the Modrinth-style card. */
    public static InstallInfo queryInfo(Context ctx, @Nullable String profileKey,
                                        String contentType, String modId,
                                        @Nullable File contentDir) {
        if (modId == null || contentType == null) return new InstallInfo(STATE_NONE, null, 0L);
        try {
            SharedPreferences p = prefs(ctx);
            String record = p.getString(key(profileKey, contentType, modId), null);
            if (record == null) return new InstallInfo(STATE_NONE, null, 0L);

            String[] parts = record.split("\\|", -1);
            String installedVersion = parts.length > 0 ? parts[0] : "";
            String fileName = parts.length > 1 ? parts[1] : "";
            long installedAt = 0L;
            if (parts.length > 2 && !parts[2].isEmpty()) {
                try { installedAt = Long.parseLong(parts[2]); } catch (NumberFormatException ignored) {}
            }

            // Auto-heal: file vanished (deleted from Manage Mods) → drop index.
            if (!fileName.isEmpty() && contentDir != null) {
                File f = new File(contentDir, fileName);
                if (!f.exists()) {
                    p.edit().remove(key(profileKey, contentType, modId)).apply();
                    return new InstallInfo(STATE_NONE, null, 0L);
                }
                // Legacy records did not store a timestamp. File mtime is the
                // closest truthful install date and avoids showing fake data.
                if (installedAt <= 0L) installedAt = f.lastModified();
            }

            String latest = p.getString(latestKey(contentType, modId), null);
            int state;
            if (latest == null || latest.isEmpty() || installedVersion.isEmpty()
                    || latest.equals(installedVersion)) {
                state = STATE_INSTALLED;
            } else {
                int cmp = compareVersionStrings(installedVersion, latest);
                state = cmp >= 0 ? STATE_INSTALLED_NEWER : STATE_UPDATE_AVAILABLE;
            }
            return new InstallInfo(state,
                    installedVersion.isEmpty() ? null : installedVersion, installedAt);
        } catch (Exception e) {
            return new InstallInfo(STATE_NONE, null, 0L);
        }
    }

    /** Dotted numeric version compare ("1.20.4" vs "1.21"); non-numeric → text order. */
    static int compareVersionStrings(String v1, String v2) {
        String c1 = v1.replaceAll("[^0-9.]", "").replaceAll("\\.$", "");
        String c2 = v2.replaceAll("[^0-9.]", "").replaceAll("\\.$", "");
        if (c1.isEmpty() || c2.isEmpty()) return v1.compareTo(v2);
        String[] p1 = c1.split("\\.");
        String[] p2 = c2.split("\\.");
        int len = Math.max(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            int n1 = 0, n2 = 0;
            try { n1 = i < p1.length ? Integer.parseInt(p1[i]) : 0; } catch (NumberFormatException ignored) {}
            try { n2 = i < p2.length ? Integer.parseInt(p2[i]) : 0; } catch (NumberFormatException ignored) {}
            if (n1 != n2) return n1 < n2 ? -1 : 1;
        }
        return 0;
    }
}
