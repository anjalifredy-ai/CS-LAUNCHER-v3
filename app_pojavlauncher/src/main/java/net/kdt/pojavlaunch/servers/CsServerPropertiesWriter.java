package net.kdt.pojavlaunch.servers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Writes full server.properties. online-mode is always written explicitly.
 * Restart required for most keys to take effect in Minecraft.
 */
public final class CsServerPropertiesWriter {
    private CsServerPropertiesWriter() {}

    public static void write(File dir, LocalServerEntry e) throws Exception {
        File props = new File(dir, "server.properties");
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(props), StandardCharsets.UTF_8)) {
            w.write("# CS Launcher — written on prepare/start/settings save\n");
            w.write("server-port=" + e.port + "\n");
            w.write("server-ip=0.0.0.0\n");
            w.write("max-players=" + Math.max(1, e.maxPlayers) + "\n");
            w.write("online-mode=" + e.onlineMode + "\n");
            w.write("white-list=" + e.whitelist + "\n");
            w.write("enforce-whitelist=" + e.whitelist + "\n");
            String motd = e.motdLine().replace("\n", " ").replace("=", "-");
            w.write("motd=" + motd + "\n");
            w.write("difficulty=" + nz(e.difficulty, "normal") + "\n");
            w.write("gamemode=" + nz(e.gamemode, "survival") + "\n");
            w.write("force-gamemode=" + e.forceGamemode + "\n");
            w.write("hardcore=" + e.hardcore + "\n");
            w.write("pvp=" + e.pvp + "\n");
            w.write("allow-flight=" + e.allowFlight + "\n");
            w.write("allow-nether=" + e.allowNether + "\n");
            w.write("spawn-monsters=" + e.spawnMonsters + "\n");
            w.write("enable-command-block=" + e.enableCommandBlock + "\n");
            w.write("generate-structures=" + e.generateStructures + "\n");
            w.write("enable-status=" + e.enableStatus + "\n");
            w.write("hide-online-players=" + e.hideOnlinePlayers + "\n");
            int vd = e.viewDistance > 0 ? e.viewDistance : 8;
            int sd = e.simulationDistance > 0 ? e.simulationDistance : 6;
            w.write("view-distance=" + clamp(vd, 3, 32) + "\n");
            w.write("simulation-distance=" + clamp(sd, 3, 32) + "\n");
            w.write("spawn-protection=" + Math.max(0, e.spawnProtection) + "\n");
            w.write("player-idle-timeout=" + Math.max(0, e.playerIdleTimeout) + "\n");
            w.write("op-permission-level=" + clamp(e.opPermissionLevel, 1, 4) + "\n");
            String level = e.worldName != null && !e.worldName.isEmpty() ? e.worldName : "world";
            w.write("level-name=" + level + "\n");
            w.write("level-type=" + nz(e.levelType, "minecraft:normal") + "\n");
            if (e.levelSeed != null && !e.levelSeed.trim().isEmpty()) {
                w.write("level-seed=" + e.levelSeed.trim() + "\n");
            }
            w.write("max-tick-time=60000\n");
            w.write("network-compression-threshold=256\n");
            w.write("enable-query=false\n");
            w.write("enable-rcon=false\n");
            w.write("sync-chunk-writes=true\n");
        }
    }

    private static String nz(String s, String d) {
        return s != null && !s.isEmpty() ? s : d;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
