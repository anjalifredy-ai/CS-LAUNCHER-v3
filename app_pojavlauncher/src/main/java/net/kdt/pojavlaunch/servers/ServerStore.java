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

    /** Pending 1-click launch for profile */
    public static void setPending(Context ctx, String profileKey, String address) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PENDING + profileKey, address).apply();
    }
    public static String getPending(Context ctx, String profileKey) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PENDING + profileKey, null);
    }
    public static void clearPending(Context ctx, String profileKey) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_PENDING + profileKey).apply();
    }
}
