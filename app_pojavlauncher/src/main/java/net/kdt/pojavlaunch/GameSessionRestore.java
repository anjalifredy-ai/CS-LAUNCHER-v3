package net.kdt.pojavlaunch;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Remembers last profile / world / server so after a mod-driven restart
 * the player can rejoin quickly. Does not inject into the MC process.
 */
public final class GameSessionRestore {
    private static final String PREF = "remom_java_session";
    private static final String K_PROFILE = "profile_key";
    private static final String K_WORLD = "last_world";
    private static final String K_SERVER = "last_server";
    private static final String K_TS = "ts";

    private GameSessionRestore() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void saveProfile(Context c, String profileKey) {
        if (c == null || profileKey == null) return;
        p(c).edit().putString(K_PROFILE, profileKey).putLong(K_TS, System.currentTimeMillis()).apply();
    }

    public static void saveWorld(Context c, String worldName) {
        if (c == null) return;
        p(c).edit().putString(K_WORLD, worldName != null ? worldName : "")
                .putLong(K_TS, System.currentTimeMillis()).apply();
    }

    public static void saveServer(Context c, String address) {
        if (c == null) return;
        p(c).edit().putString(K_SERVER, address != null ? address : "")
                .putLong(K_TS, System.currentTimeMillis()).apply();
    }

    public static String lastProfile(Context c) {
        return c == null ? null : p(c).getString(K_PROFILE, null);
    }

    public static String lastWorld(Context c) {
        return c == null ? null : p(c).getString(K_WORLD, null);
    }

    public static String lastServer(Context c) {
        return c == null ? null : p(c).getString(K_SERVER, null);
    }

    public static String resumeHint(Context c) {
        String w = lastWorld(c);
        String s = lastServer(c);
        if (w != null && !w.isEmpty()) return "Last world: " + w + " — open it from Singleplayer.";
        if (s != null && !s.isEmpty()) return "Last server: " + s + " — reconnect from Multiplayer.";
        return "Rejoin your world from the menu after loading.";
    }
}
