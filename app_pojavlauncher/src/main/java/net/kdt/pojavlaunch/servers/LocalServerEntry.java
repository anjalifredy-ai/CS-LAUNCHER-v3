package net.kdt.pojavlaunch.servers;

/**
 * Named local dedicated server managed inside CS Launcher.
 * Friends join via LAN IP, or worldwide via playit / port-forward public address.
 */
public class LocalServerEntry {
    public String id;
    public String name;
    /** Shown in multiplayer list as MOTD / description */
    public String description = "";
    public String versionId; // e.g. 1.21.1
    /** vanilla | paper | fabric | purpur */
    public String software = "vanilla";
    public int port = 25565;
    public int maxPlayers = 10;
    public int memoryMb = 1024;
    /** Absolute path to 64x64 PNG used as server-icon.png */
    public String iconPath;
    public boolean onlineMode = false; // offline friends friendly by default
    public boolean whitelist = false;
    public boolean pvp = true;
    public boolean hardcore = false;
    public boolean allowFlight = false;
    public String difficulty = "normal";
    public String gamemode = "survival";
    public int viewDistance = 8;
    public int simulationDistance = 6;
    public String worldName = "world";
    public boolean running;
    public long createdAt;
    /** Optional saved playit / public tunnel address for distant friends */
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
        if (description != null && !description.trim().isEmpty()) {
            return description.trim();
        }
        return name != null ? name : "CS Launcher Server";
    }

    public String softwareLabel() {
        if (software == null || software.isEmpty()) return "Vanilla";
        String s = software.toLowerCase();
        if ("paper".equals(s)) return "Paper";
        if ("fabric".equals(s)) return "Fabric";
        if ("purpur".equals(s)) return "Purpur";
        if ("forge".equals(s)) return "Forge";
        if ("neoforge".equals(s)) return "NeoForge";
        if ("quilt".equals(s)) return "Quilt";
        return "Vanilla";
    }
}
