package net.kdt.pojavlaunch.servers;

/**
 * The built-in partner server.
 *
 * <p>PlantMC is a promotion partner of CS Launcher, so its entry is shipped with the launcher:
 * it is always present at the top of the Server Hub, it cannot be deleted or edited by the user,
 * and it is drawn with its own featured card (banner artwork + Discord button).
 *
 * <p>Everything else about it behaves like a normal server — it is pinged live and it can be
 * joined with one tap.
 */
public final class PartnerServer {

    public static final String ADDRESS = "play.plantmc.fun:25627";
    public static final String HOST = "play.plantmc.fun";
    public static final int PORT = 25627;
    public static final String NAME = "PlantMC";
    public static final String TAGLINE = "Survival \u2022 Farming \u2022 Economy";
    public static final String DISCORD_URL = "https://discord.gg/plantmc";

    private PartnerServer() {}

    /** A fresh, unpinged copy of the partner entry. */
    public static ServerEntry create() {
        ServerEntry e = new ServerEntry(ADDRESS);
        e.name = NAME;
        e.pinned = true;
        e.motd = TAGLINE;
        return e;
    }

    /** True when the given address belongs to the partner server (any port spelling). */
    public static boolean is(String address) {
        if (address == null) return false;
        String a = address.trim().toLowerCase();
        if (a.startsWith("http://")) a = a.substring(7);
        if (a.startsWith("https://")) a = a.substring(8);
        if (a.endsWith("/")) a = a.substring(0, a.length() - 1);
        return a.equals(ADDRESS) || a.equals(HOST) || a.equals(HOST + ":25565");
    }
}
