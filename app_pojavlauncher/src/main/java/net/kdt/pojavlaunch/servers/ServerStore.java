package net.kdt.pojavlaunch.servers;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists servers globally (and per-profile selected). Dark theme uses same store.
 * Backed by SharedPreferences for light weight; servers.json style but via prefs.
 */
public class ServerStore {
    private static final String PREFS = "cs_servers_v31";
    private static final String KEY_LIST = "servers";
    private static final String KEY_PENDING = "pending_server_"; // + profileKey
    private static final String KEY_PENDING_GLOBAL = "pending_server_global";
    private static final String KEY_PENDING_TIME = "pending_server_time";
    private static final Gson GSON = new Gson();

    public static List<ServerEntry> load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString(KEY_LIST, null);
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<ServerEntry>>(){}.getType();
            List<ServerEntry> l = GSON.fromJson(json, t);
            return l != null ? l : new ArrayList<>();
        } catch (Exception e) { return new ArrayList<>(); }
    }

    public static void save(Context ctx, List<ServerEntry> list) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = GSON.toJson(list);
        p.edit().putString(KEY_LIST, json).apply();
    }

    public static void add(Context ctx, ServerEntry e) {
        List<ServerEntry> l = load(ctx);
        // avoid duplicates by address (case-insensitive)
        for (ServerEntry s : l) if (s.address.equalsIgnoreCase(e.address)) return;
        l.add(0, e);
        save(ctx, l);
    }

    public static void remove(Context ctx, String address) {
        List<ServerEntry> l = load(ctx);
        l.removeIf(s -> s.address.equalsIgnoreCase(address));
        save(ctx, l);
    }

    public static void update(Context ctx, ServerEntry updated) {
        List<ServerEntry> l = load(ctx);
        for (int i=0;i<l.size();i++) if (l.get(i).address.equalsIgnoreCase(updated.address)) { l.set(i, updated); break; }
        save(ctx, l);
    }

    /**
     * Pending 1-click launch.
     *
     * <p>A global copy is always written next to the per-profile one. The launch path only knows
     * the "current profile" preference, and that key can differ from the profile the hub was
     * opened from (shortcuts, quick-launch, profile switch after queueing) — which is exactly
     * why auto-join used to silently do nothing. The global key is the fallback that makes the
     * hand-off reliable, and it is cleared as soon as the game is launched.
     */
    public static void setPending(Context ctx, String profileKey, String address) {
        SharedPreferences.Editor ed = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (profileKey != null) ed.putString(KEY_PENDING + profileKey, address);
        ed.putString(KEY_PENDING_GLOBAL, address);
        ed.putLong(KEY_PENDING_TIME, System.currentTimeMillis());
        ed.apply();
    }

    public static String getPending(Context ctx, String profileKey) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String perProfile = profileKey != null ? p.getString(KEY_PENDING + profileKey, null) : null;
        if (perProfile != null && !perProfile.isEmpty()) return perProfile;
        return p.getString(KEY_PENDING_GLOBAL, null);
    }

    public static void clearPending(Context ctx, String profileKey) {
        SharedPreferences.Editor ed = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (profileKey != null) ed.remove(KEY_PENDING + profileKey);
        ed.remove(KEY_PENDING_GLOBAL).remove(KEY_PENDING_TIME);
        ed.apply();
    }

    /** Clears the queued join for every profile — used once the game has actually been started. */
    public static void clearAllPending(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = p.edit();
        for (String key : p.getAll().keySet()) if (key.startsWith(KEY_PENDING)) ed.remove(key);
        ed.remove(KEY_PENDING_GLOBAL).remove(KEY_PENDING_TIME);
        ed.apply();
    }
}
