package net.kdt.pojavlaunch.servers;

import android.content.Context;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;
import net.kdt.pojavlaunch.worlds.NbtIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps {@code <gameDir>/servers.dat} in sync with the launcher Server Hub.
 *
 * CRITICAL: never overwrite / rebuild servers.dat when the existing file fails to parse.
 * That was wiping servers the player added in Multiplayer on the next launch.
 */
public class ServerListSync {
    private static final String TAG = "ServerListSync";

    public static void syncToGameDir(Context ctx, String profileKey) {
        // Pull in-game servers into hub first so nothing is lost across launches
        importFromGameDir(ctx, profileKey);
        syncToGameDir(ctx, profileKey, resolveGameDir(profileKey));
    }

    public static void syncToGameDir(Context ctx, String profileKey, File gameDir) {
        try {
            if (gameDir == null) gameDir = new File(Tools.DIR_GAME_NEW);
            if (!gameDir.exists() && !gameDir.mkdirs()) {
                Log.w(TAG, "cannot create game dir " + gameDir);
                return;
            }
            // Always import first when we know the exact gameDir
            importFromGameDir(ctx, profileKey, gameDir);

            List<ServerEntry> hubServers = ServerStore.load(ctx, profileKey);
            File dat = new File(gameDir, "servers.dat");

            NbtIO.NbtList list = new NbtIO.NbtList(10 /* TAG_Compound */);
            Set<String> known = new LinkedHashSet<>();
            boolean existingOk = false;
            if (dat.isFile()) {
                try {
                    Map<String, Object> existing = NbtIO.readPossiblyGzipped(dat);
                    Object serversTag = existing.get("servers");
                    if (serversTag instanceof NbtIO.NbtList) {
                        for (Object item : ((NbtIO.NbtList) serversTag).items) {
                            if (!(item instanceof Map)) continue;
                            @SuppressWarnings("unchecked")
                            Map<String, Object> entry = (Map<String, Object>) item;
                            Object ip = entry.get("ip");
                            if (ip instanceof String) known.add(((String) ip).toLowerCase());
                            list.items.add(entry);
                        }
                        existingOk = true;
                    }
                } catch (Exception readFail) {
                    // NEVER wipe the file. Leave Minecraft's servers.dat alone.
                    Log.w(TAG, "servers.dat unreadable — leaving file untouched: " + readFail.getMessage());
                    return;
                }
            }

            boolean changed = false;
            for (ServerEntry e : hubServers) {
                if (e == null || e.address == null || e.address.isEmpty()) continue;
                String key = e.address.toLowerCase();
                if (known.contains(key)) continue;
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("ip", e.address);
                String name = e.name != null && !e.name.isEmpty() ? e.name : e.address;
                name = MotdRenderer.stripLegacy(name).trim();
                if (name.isEmpty()) name = e.address;
                if (name.length() > 64) name = name.substring(0, 64);
                entry.put("name", name);
                // no icon tag (byte[] only since 1.20.5; string icons break the whole list)
                entry.put("acceptTextures", (byte) 1);
                list.items.add(entry);
                known.add(key);
                changed = true;
            }

            if (!changed) return; // keep existing file as-is

            // Backup before write
            if (dat.isFile() && existingOk) {
                try {
                    File bak = new File(gameDir, "servers.dat.bak");
                    copyFile(dat, bak);
                } catch (Exception ignored) {}
            }

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("servers", list);
            NbtIO.writeGzipped(dat, root);
            Log.i(TAG, "servers.dat synced (" + list.items.size() + " entries) -> " + dat);
        } catch (Exception ex) {
            Log.w(TAG, "sync failed", ex);
        }
    }

    public static void importFromGameDir(Context ctx, String profileKey) {
        importFromGameDir(ctx, profileKey, resolveGameDir(profileKey));
    }

    public static void importFromGameDir(Context ctx, String profileKey, File gameDir) {
        try {
            if (gameDir == null) return;
            File dat = new File(gameDir, "servers.dat");
            if (!dat.isFile()) return;
            Map<String, Object> root;
            try {
                root = NbtIO.readPossiblyGzipped(dat);
            } catch (Exception e) {
                Log.w(TAG, "import skip (unreadable): " + e.getMessage());
                return;
            }
            Object serversTag = root.get("servers");
            if (!(serversTag instanceof NbtIO.NbtList)) return;

            List<ServerEntry> current = ServerStore.loadUserServers(ctx, profileKey);
            Set<String> known = new LinkedHashSet<>();
            for (ServerEntry e : current) if (e.address != null) known.add(e.address.toLowerCase());

            List<ServerEntry> imported = new ArrayList<>();
            for (Object item : ((NbtIO.NbtList) serversTag).items) {
                if (!(item instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) item;
                Object ip = entry.get("ip");
                if (!(ip instanceof String)) continue;
                String address = ((String) ip).trim();
                if (address.isEmpty() || known.contains(address.toLowerCase())) continue;
                ServerEntry se = new ServerEntry(address);
                Object name = entry.get("name");
                if (name instanceof String && !((String) name).isEmpty()) se.name = (String) name;
                imported.add(se);
                known.add(address.toLowerCase());
            }
            if (imported.isEmpty()) return;
            current.addAll(imported);
            ServerStore.save(ctx, profileKey, current);
            Log.i(TAG, "imported " + imported.size() + " server(s) from servers.dat");
        } catch (Exception ex) {
            Log.w(TAG, "import failed", ex);
        }
    }

    public static void syncAllProfiles(Context ctx) {
        try {
            LauncherProfiles.load();
            for (String key : LauncherProfiles.mainProfileJson.profiles.keySet()) {
                syncToGameDir(ctx, key);
            }
        } catch (Exception ignored) {}
    }

    private static File resolveGameDir(String profileKey) {
        File gameDir = null;
        if (profileKey != null) {
            try {
                LauncherProfiles.load();
                MinecraftProfile p = LauncherProfiles.mainProfileJson.profiles.get(profileKey);
                if (p != null) gameDir = Tools.getGameDirPath(p);
            } catch (Exception ignored) {}
        }
        if (gameDir == null) gameDir = new File(Tools.DIR_GAME_NEW);
        return gameDir;
    }

    private static void copyFile(File src, File dst) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }
}
