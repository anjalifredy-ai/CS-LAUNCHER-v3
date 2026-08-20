package net.kdt.pojavlaunch.servers;

import android.content.Context;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;
import net.kdt.pojavlaunch.worlds.NbtIO;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zalith-style: keep in-game servers.dat in sync with hub's ServerStore.
 * So a server added in launcher also appears in Multiplayer list.
 */
public class ServerListSync {
    public static void syncToGameDir(Context ctx, String profileKey) {
        try {
            List<ServerEntry> servers = ServerStore.load(ctx);
            if (servers.isEmpty()) return;
            // Resolve game dir for profile
            File gameDir = null;
            if (profileKey != null) {
                try {
                    LauncherProfiles.load();
                    MinecraftProfile p = LauncherProfiles.mainProfileJson.profiles.get(profileKey);
                    if (p != null) gameDir = Tools.getGameDirPath(p);
                } catch (Exception ignored) {}
            }
            if (gameDir == null) gameDir = new File(Tools.DIR_GAME_NEW);
            if (!gameDir.exists()) gameDir.mkdirs();
            File dat = new File(gameDir, "servers.dat");
            // Build NBT: root compound with List "servers" of compounds {ip, name, icon}
            Map<String, Object> root = new LinkedHashMap<>();
            NbtIO.NbtList list = new NbtIO.NbtList(10); // TAG_Compound
            for (ServerEntry e : servers) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("ip", e.address);
                // Use MOTD first line or name as server name, as Minecraft does
                String name = e.name != null && !e.name.isEmpty() ? e.name : e.address;
                // Minecraft's server list truncates name at  next? Keep as is
                entry.put("name", name);
                if (e.iconBase64 != null && e.iconBase64.startsWith("data:image")) {
                    // NBT expects base64 without prefix? Actually it stores the whole data URI string
                    entry.put("icon", e.iconBase64);
                }
                // Also store hidden? We keep it simple
                list.items.add(entry);
            }
            root.put("servers", list);
            NbtIO.writeGzipped(dat, root);
        } catch (Exception ex) {
            android.util.Log.w("ServerListSync", "sync failed", ex);
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
}
