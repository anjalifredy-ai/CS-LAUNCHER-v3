package net.kdt.pojavlaunch.servers;

/** Named local dedicated server managed inside Remom (no website). */
public class LocalServerEntry {
    public String id;
    public String name;
    public String versionId; // e.g. 1.21.1
    public int port = 25565;
    public int maxPlayers = 10;
    public boolean onlineMode = false; // offline friends friendly
    public boolean running;
    public long createdAt;

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
}
