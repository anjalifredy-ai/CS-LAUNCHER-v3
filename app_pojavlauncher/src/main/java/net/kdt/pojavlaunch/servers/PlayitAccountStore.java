package net.kdt.pojavlaunch.servers;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * One-time playit link for CS Launcher.
 * After the user links once and saves their public tunnel address,
 * every local server can reuse it automatically on Start.
 *
 * Full native playit agent (binary) is a follow-up; this gives Feather-style
 * UX: sign-in once → paste public address once → all servers get it.
 */
public final class PlayitAccountStore {
    private static final String PREF = "cs_playit_account";
    private static final String KEY_LINKED = "linked";
    private static final String KEY_DEFAULT_ADDR = "default_public_address";
    private static final String KEY_AGENT_NOTE = "agent_note";

    private PlayitAccountStore() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean isLinked(Context ctx) {
        return prefs(ctx).getBoolean(KEY_LINKED, false);
    }

    public static void setLinked(Context ctx, boolean linked) {
        prefs(ctx).edit().putBoolean(KEY_LINKED, linked).apply();
    }

    /** e.g. abc123.playit.gg:25565 or region.playit.gg:12345 */
    public static String getDefaultPublicAddress(Context ctx) {
        return prefs(ctx).getString(KEY_DEFAULT_ADDR, "");
    }

    public static void setDefaultPublicAddress(Context ctx, String address) {
        String a = address != null ? address.trim() : "";
        prefs(ctx).edit()
                .putString(KEY_DEFAULT_ADDR, a)
                .putBoolean(KEY_LINKED, !a.isEmpty())
                .apply();
    }

    public static void clear(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }

    /**
     * When a local server starts: if it has no publicAddress yet but account
     * has a default playit address, copy it onto the server entry.
     */
    public static boolean applyDefaultToServer(Context ctx, LocalServerEntry e) {
        if (e == null) return false;
        if (e.publicAddress != null && !e.publicAddress.trim().isEmpty()) return false;
        String def = getDefaultPublicAddress(ctx);
        if (def == null || def.isEmpty()) return false;
        e.publicAddress = def;
        LocalServerStore.update(ctx, e);
        return true;
    }

    public static String statusLine(Context ctx) {
        if (!isLinked(ctx)) {
            return "Playit: not linked — open once, create tunnel, paste address";
        }
        String a = getDefaultPublicAddress(ctx);
        if (a == null || a.isEmpty()) {
            return "Playit: linked — paste public address to auto-apply";
        }
        return "Playit: ON → " + a;
    }
}
