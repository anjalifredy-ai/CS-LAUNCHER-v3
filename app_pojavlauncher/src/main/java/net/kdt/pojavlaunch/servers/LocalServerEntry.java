package net.kdt.pojavlaunch.servers;

/**
 * Local dedicated server managed inside CS Launcher.
 * onlineMode=false → offline / cracked friends can join (allow offline players).
 * onlineMode=true  → only legitimate Microsoft accounts.
 */
public class LocalServerEntry {
    public String id;
    public String name;
    public String description = "";
    public String versionId;
    /** vanilla | paper | fabric | purpur — OptiFine is client-only, not a server jar */
    public String software = "vanilla";
    public int port = 25565;
    public int maxPlayers = 10;
    public int memoryMb = 1024;
    public String iconPath;
    /** false = allow offline players (cracked / offline launcher names) */
    public boolean onlineMode = false;
    public boolean whitelist = false;
    public boolean pvp = true;
    public boolean hardcore = false;
    public boolean allowFlight = false;
    public boolean forceGamemode = false;
    public boolean generateStructures = true;
    public boolean enableCommandBlock = true;
    public boolean spawnMonsters = true;
    public boolean allowNether = true;
    public boolean enableStatus = true;
    public boolean hideOnlinePlayers = false;
    public String difficulty = "normal";
    public String gamemode = "survival";
    public String levelType = "minecraft:normal";
    public String levelSeed = "";
    public int viewDistance = 8;
    public int simulationDistance = 6;
    public int spawnProtection = 0;
    public int playerIdleTimeout = 0;
    public int opPermissionLevel = 4;
    public String worldName = "world";
    public boolean running;
    public long createdAt;
    public String publicAddress;

    public LocalServerEntry() {}

    public LocalServerEntry(String id, String name, String versionId) {
        this.id = id;
        this.name = name;
        this.versionId = versionId;
        this.createdAt = System.currentTimeMillis();
    }

    public String displayAddress(String lanIp) {
        String ip = (lanIp != null && !lanIp.isEmpty()) ? lanIp : "127.0.0.1";
        return ip + ":" + port;
    }

    public String motdLine() {
        if (description != null && !description.trim().isEmpty()) return description.trim();
        return name != null ? name : "CS Launcher Server";
    }

    public String softwareLabel() {
        if (software == null || software.isEmpty()) return "Vanilla";
        switch (software.toLowerCase()) {
            case "paper": return "Paper";
            case "fabric": return "Fabric";
            case "purpur": return "Purpur";
            case "forge": return "Forge";
            case "neoforge": return "NeoForge";
            default: return "Vanilla";
        }
    }
}
