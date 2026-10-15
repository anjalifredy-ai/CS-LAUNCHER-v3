package net.kdt.pojavlaunch.servers;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LocalServerStore {
    private static final String PREFS = "remom_local_servers_v1";
    private static final String KEY = "servers";
    private static final Gson GSON = new Gson();

    private LocalServerStore() {}

    public static List<LocalServerEntry> load(Context ctx) {
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<LocalServerEntry>>(){}.getType();
            List<LocalServerEntry> list = GSON.fromJson(json, t);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void save(Context ctx, List<LocalServerEntry> list) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, GSON.toJson(list)).apply();
    }

    public static LocalServerEntry create(Context ctx, String name, String versionId, int port) {
        List<LocalServerEntry> list = load(ctx);
        LocalServerEntry e = new LocalServerEntry(
                UUID.randomUUID().toString().substring(0, 8),
                name == null || name.trim().isEmpty() ? "Remom Server" : name.trim(),
                versionId == null || versionId.isEmpty() ? "1.20.4" : versionId.trim()
        );
        e.port = port > 0 ? port : 25565;
        e.onlineMode = false;
        list.add(0, e);
        save(ctx, list);
        return e;
    }

    public static void update(Context ctx, LocalServerEntry updated) {
        List<LocalServerEntry> list = load(ctx);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id != null && list.get(i).id.equals(updated.id)) {
                list.set(i, updated);
                break;
            }
        }
        save(ctx, list);
    }

    public static void remove(Context ctx, String id) {
        List<LocalServerEntry> list = load(ctx);
        list.removeIf(s -> s.id != null && s.id.equals(id));
        save(ctx, list);
    }
}
