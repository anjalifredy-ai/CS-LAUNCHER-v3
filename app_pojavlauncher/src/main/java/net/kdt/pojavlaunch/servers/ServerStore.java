package net.kdt.pojavlaunch.servers;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Server storage.
 *
 * <p>Two rules shape this class:
 * <ul>
 *   <li><b>Per profile.</b> Every launcher profile owns its own server list, keyed by the profile
 *       key, so a server added while editing "1.21 Fabric" never shows up under "1.8.9 PvP".</li>
 *   <li><b>The built-in server is global.</b> It lives in its own slot, is returned first for
 *       every profile, and cannot be removed, edited or duplicated by list operations.</li>
 * </ul>
 */
public class ServerStore {
    private static final String PREFS = "cs_servers_v31";
    /** Legacy single global list (pre per-profile builds); migrated on first read. */
    private static final String KEY_LIST = "servers";
    private static final String KEY_LIST_PROFILE = "servers_profile_"; // + profileKey
    private static final String KEY_MIGRATED = "servers_migrated_"; // + profileKey
    private static final String KEY_BUILTIN = "builtin_cache";
    private static final String KEY_PENDING = "pending_server_"; // + profileKey
    private static final String KEY_PENDING_GLOBAL = "pending_server_global";
    private static final String KEY_PENDING_TIME = "pending_server_time";
    private static final Gson GSON = new Gson();

    private static String listKey(String profileKey) {
        return KEY_LIST_PROFILE + (profileKey == null || profileKey.isEmpty() ? "default" : profileKey);
    }

    /**
     * Built-in server first, then this profile's own servers.
     */
    public static List<ServerEntry> load(Context ctx, String profileKey) {
        List<ServerEntry> out = new ArrayList<>();
        out.add(loadBuiltIn(ctx));
        out.addAll(loadUserServers(ctx, profileKey));
        return out;
    }

    public static List<ServerEntry> loadUserServers(Context ctx, String profileKey) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = listKey(profileKey);
        String json = p.getString(key, null);

        // One-time migration: the old builds kept a single shared list. Copy it into the first
        // profile that asks for it so nobody loses the servers they already added.
        if (json == null) {
            String legacy = p.getString(KEY_LIST, null);
            boolean migrated = p.getBoolean(KEY_MIGRATED + key, false);
            if (legacy != null && !legacy.isEmpty() && !migrated) {
                p.edit().putString(key, legacy).putBoolean(KEY_MIGRATED + key, true).apply();
                json = legacy;
            }
        }
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<ServerEntry>>(){}.getType();
            List<ServerEntry> l = GSON.fromJson(json, t);
            if (l == null) return new ArrayList<>();
            l.removeIf(s -> s == null || s.address == null || PartnerServer.is(s.address));
            return l;
        } catch (Exception e) { return new ArrayList<>(); }
    }

    /** The built-in entry plus its last known ping / MOTD / icon cache. */
    public static ServerEntry loadBuiltIn(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString(KEY_BUILTIN, null);
        ServerEntry e = null;
        if (json != null && !json.isEmpty()) {
            try { e = GSON.fromJson(json, ServerEntry.class); } catch (Exception ignored) {}
        }
        if (e == null || e.address == null) e = PartnerServer.create();
        // Address and pinned state are owned by the launcher, never by the cache.
        e.address = PartnerServer.ADDRESS;
        e.pinned = true;
        if (e.name == null || e.name.isEmpty() || e.name.equals(e.address)) e.name = PartnerServer.NAME;
        return e;
    }

    private static void saveBuiltIn(Context ctx, ServerEntry e) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putString(KEY_BUILTIN, GSON.toJson(e)).apply();
    }

    /** Persists this profile's own servers; the built-in entry is routed to its own slot. */
    public static void save(Context ctx, String profileKey, List<ServerEntry> list) {
        List<ServerEntry> userOnly = new ArrayList<>();
        for (ServerEntry s : list) {
            if (s == null || s.address == null) continue;
            if (s.pinned || PartnerServer.is(s.address)) { saveBuiltIn(ctx, s); continue; }
            userOnly.add(s);
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putString(listKey(profileKey), GSON.toJson(userOnly)).apply();
    }

    public static void add(Context ctx, String profileKey, ServerEntry e) {
        if (e == null || e.address == null) return;
        if (PartnerServer.is(e.address)) { saveBuiltIn(ctx, e); return; } // already built in
        List<ServerEntry> l = loadUserServers(ctx, profileKey);
        for (ServerEntry s : l) if (s.address.equalsIgnoreCase(e.address)) return;
        l.add(0, e);
        save(ctx, profileKey, l);
    }

    /** @return false when the address is the built-in server, which cannot be removed. */
    public static boolean remove(Context ctx, String profileKey, String address) {
        if (PartnerServer.is(address)) return false;
        List<ServerEntry> l = loadUserServers(ctx, profileKey);
        l.removeIf(s -> s.address.equalsIgnoreCase(address));
        save(ctx, profileKey, l);
        return true;
    }

    public static void update(Context ctx, String profileKey, ServerEntry updated) {
        if (updated == null || updated.address == null) return;
        if (updated.pinned || PartnerServer.is(updated.address)) { saveBuiltIn(ctx, updated); return; }
        List<ServerEntry> l = loadUserServers(ctx, profileKey);
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).address.equalsIgnoreCase(updated.address)) { l.set(i, updated); break; }
        }
        save(ctx, profileKey, l);
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
