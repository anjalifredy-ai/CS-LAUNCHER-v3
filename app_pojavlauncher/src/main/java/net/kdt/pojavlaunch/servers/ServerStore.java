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
    private static final String KEY_PARTNER = "partner_cache";
    private static final String KEY_PENDING = "pending_server_"; // + profileKey
    private static final String KEY_PENDING_GLOBAL = "pending_server_global";
    private static final String KEY_PENDING_TIME = "pending_server_time";
    private static final Gson GSON = new Gson();

    /**
     * Returns the partner server first, followed by the user's own servers.
     * The partner entry lives in its own preference slot, so it can never be lost, duplicated
     * or deleted by list operations.
     */
    public static List<ServerEntry> load(Context ctx) {
        List<ServerEntry> out = new ArrayList<>();
        out.add(loadPartner(ctx));
        out.addAll(loadUserServers(ctx));
        return out;
    }

    public static List<ServerEntry> loadUserServers(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString(KEY_LIST, null);
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<ServerEntry>>(){}.getType();
            List<ServerEntry> l = GSON.fromJson(json, t);
            if (l == null) return new ArrayList<>();
            // Defensive: an old build may have persisted the partner into the user list.
            l.removeIf(s -> s == null || s.address == null || PartnerServer.is(s.address));
            return l;
        } catch (Exception e) { return new ArrayList<>(); }
    }

    /** Partner entry with its last known ping/MOTD cache. */
    public static ServerEntry loadPartner(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString(KEY_PARTNER, null);
        ServerEntry e = null;
        if (json != null && !json.isEmpty()) {
            try { e = GSON.fromJson(json, ServerEntry.class); } catch (Exception ignored) {}
        }
        if (e == null || e.address == null) e = PartnerServer.create();
        // The address, the name and the pinned flag are owned by the launcher, not by the cache.
        e.address = PartnerServer.ADDRESS;
        e.pinned = true;
        if (e.name == null || e.name.isEmpty() || e.name.equals(e.address)) e.name = PartnerServer.NAME;
        return e;
    }

    private static void savePartner(Context ctx, ServerEntry e) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putString(KEY_PARTNER, GSON.toJson(e)).apply();
    }

    /** Persists the user list only — the partner entry is filtered out on purpose. */
    public static void save(Context ctx, List<ServerEntry> list) {
        List<ServerEntry> userOnly = new ArrayList<>();
        for (ServerEntry s : list) {
            if (s == null || s.address == null) continue;
            if (s.pinned || PartnerServer.is(s.address)) { savePartner(ctx, s); continue; }
            userOnly.add(s);
        }
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString(KEY_LIST, GSON.toJson(userOnly)).apply();
    }

    public static void add(Context ctx, ServerEntry e) {
        if (e == null || e.address == null) return;
        if (PartnerServer.is(e.address)) { savePartner(ctx, e); return; } // already built in
        List<ServerEntry> l = loadUserServers(ctx);
        for (ServerEntry s : l) if (s.address.equalsIgnoreCase(e.address)) return;
        l.add(0, e);
        save(ctx, l);
    }

    /** @return false when the address is the partner server, which cannot be removed. */
    public static boolean remove(Context ctx, String address) {
        if (PartnerServer.is(address)) return false;
        List<ServerEntry> l = loadUserServers(ctx);
        l.removeIf(s -> s.address.equalsIgnoreCase(address));
        save(ctx, l);
        return true;
    }

    public static void update(Context ctx, ServerEntry updated) {
        if (updated == null || updated.address == null) return;
        if (updated.pinned || PartnerServer.is(updated.address)) { savePartner(ctx, updated); return; }
        List<ServerEntry> l = loadUserServers(ctx);
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
