package net.kdt.pojavlaunch.server;

/**
 * Persistent configuration for one on-device Minecraft Java server.
 * World and files stay local on the device.
 */
public class CsServerConfig {
    public String id;
    public String name;
    public String description = "";
    public String motd = "A CS Launcher Minecraft Server";
    public String iconPath;
    public String minecraftVersion;
    public String software; // CsServerSoftware name
    public String softwareVersion;
    public String serverDir;
    public String worldDir = "world";
    public int port = 25565;
    public String bindAddress = "0.0.0.0";
    public int maxPlayers = 10;
    public int memoryMb = 1024;
    public boolean onlineMode = true;
    public boolean whitelist = false;
    public String difficulty = "normal";
    public String gamemode = "survival";
    public int viewDistance = 8;
    public int simulationDistance = 6;
    public boolean pvp = true;
    public boolean hardcore = false;
    public boolean allowFlight = false;
    public boolean enableCommandBlocks = false;
    public int spawnProtection = 16;
    public boolean eulaAccepted = false;
    public String jvmArgs = "";
    public long createdAt = System.currentTimeMillis();
    public long updatedAt = System.currentTimeMillis();

    public CsServerConfig() {}

    public CsServerSoftware softwareEnum() {
        return CsServerSoftware.fromName(software);
    }

    public CsServerConfig copy() {
        CsServerConfig c = new CsServerConfig();
        c.id = id;
        c.name = name;
        c.description = description;
        c.motd = motd;
        c.iconPath = iconPath;
        c.minecraftVersion = minecraftVersion;
        c.software = software;
        c.softwareVersion = softwareVersion;
        c.serverDir = serverDir;
        c.worldDir = worldDir;
        c.port = port;
        c.bindAddress = bindAddress;
        c.maxPlayers = maxPlayers;
        c.memoryMb = memoryMb;
        c.onlineMode = onlineMode;
        c.whitelist = whitelist;
        c.difficulty = difficulty;
        c.gamemode = gamemode;
        c.viewDistance = viewDistance;
        c.simulationDistance = simulationDistance;
        c.pvp = pvp;
        c.hardcore = hardcore;
        c.allowFlight = allowFlight;
        c.enableCommandBlocks = enableCommandBlocks;
        c.spawnProtection = spawnProtection;
        c.eulaAccepted = eulaAccepted;
        c.jvmArgs = jvmArgs;
        c.createdAt = createdAt;
        c.updatedAt = updatedAt;
        return c;
    }
}
