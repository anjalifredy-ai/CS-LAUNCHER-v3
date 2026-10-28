package net.kdt.pojavlaunch.server;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/** Hosted dedicated-server config for CS Launcher. */
public class CsServerConfig {
    public String id;
    public String name;
    public String minecraftVersion;
    public String software; // VANILLA, PAPER, FABRIC, FORGE, NEOFORGE, QUILT, PURPUR
    public int memoryMb = 1024;
    public int port = 25565;
    public int maxPlayers = 10;
    public boolean onlineMode = false;
    public boolean whitelist = false;
    public boolean pvp = true;
    public String motd = "CS Launcher Server";
    public String serverDir;
    public String worldDir = "world";

    public static CsServerConfig createNew(String name, String version, String software, String baseDir) {
        CsServerConfig c = new CsServerConfig();
        c.id = UUID.randomUUID().toString();
        c.name = name;
        c.minecraftVersion = version;
        c.software = software;
        c.serverDir = baseDir + "/servers/" + c.id;
        return c;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("minecraftVersion", minecraftVersion);
        o.put("software", software);
        o.put("memoryMb", memoryMb);
        o.put("port", port);
        o.put("maxPlayers", maxPlayers);
        o.put("onlineMode", onlineMode);
        o.put("whitelist", whitelist);
        o.put("pvp", pvp);
        o.put("motd", motd);
        o.put("serverDir", serverDir);
        o.put("worldDir", worldDir);
        return o;
    }

    public static CsServerConfig fromJson(JSONObject o) throws JSONException {
        CsServerConfig c = new CsServerConfig();
        c.id = o.getString("id");
        c.name = o.getString("name");
        c.minecraftVersion = o.optString("minecraftVersion", "1.21.1");
        c.software = o.optString("software", "PAPER");
        c.memoryMb = o.optInt("memoryMb", 1024);
        c.port = o.optInt("port", 25565);
        c.maxPlayers = o.optInt("maxPlayers", 10);
        c.onlineMode = o.optBoolean("onlineMode", false);
        c.whitelist = o.optBoolean("whitelist", false);
        c.pvp = o.optBoolean("pvp", true);
        c.motd = o.optString("motd", "CS Launcher Server");
        c.serverDir = o.optString("serverDir", "");
        c.worldDir = o.optString("worldDir", "world");
        return c;
    }
}
