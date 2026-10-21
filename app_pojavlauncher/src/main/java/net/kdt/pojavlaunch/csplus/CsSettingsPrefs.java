package net.kdt.pojavlaunch.csplus;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lightweight prefs for CS Plus UI (Part 3).
 * Does not replace existing LauncherPreferences — only CS-themed extras.
 */
public final class CsSettingsPrefs {

    private static final String PREFS = "cs_launcher_prefs";
    private static final String KEY_MEMORY = "memory_mb";
    private static final String KEY_DARK = "dark_theme";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PROFILE = "current_profile";

    private CsSettingsPrefs() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int getMemoryMb(Context c) {
        return p(c).getInt(KEY_MEMORY, 2048);
    }

    public static void setMemoryMb(Context c, int mb) {
        p(c).edit().putInt(KEY_MEMORY, mb).apply();
    }

    public static boolean isDarkTheme(Context c) {
        return p(c).getBoolean(KEY_DARK, true);
    }

    public static void setDarkTheme(Context c, boolean dark) {
        p(c).edit().putBoolean(KEY_DARK, dark).apply();
    }

    public static String getUsername(Context c) {
        return p(c).getString(KEY_USERNAME, "Player");
    }

    public static void setUsername(Context c, String name) {
        p(c).edit().putString(KEY_USERNAME, name == null ? "Player" : name).apply();
    }

    public static String getCurrentProfileId(Context c) {
        return p(c).getString(KEY_PROFILE, "default");
    }

    public static void setCurrentProfileId(Context c, String id) {
        p(c).edit().putString(KEY_PROFILE, id == null ? "default" : id).apply();
    }
}
