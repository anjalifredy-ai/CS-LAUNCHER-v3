package net.kdt.pojavlaunch.server;

/**
 * Supported Minecraft Java server software for on-device hosting.
 */
public enum CsServerSoftware {
    VANILLA("Vanilla", false, false),
    FABRIC("Fabric", false, true),
    PAPER("Paper", true, false),
    PURPUR("Purpur", true, false),
    SPIGOT("Spigot", true, false),
    FORGE("Forge", false, true),
    NEOFORGE("NeoForge", false, true),
    QUILT("Quilt", false, true);

    public final String displayName;
    public final boolean supportsPlugins;
    public final boolean supportsMods;

    CsServerSoftware(String displayName, boolean supportsPlugins, boolean supportsMods) {
        this.displayName = displayName;
        this.supportsPlugins = supportsPlugins;
        this.supportsMods = supportsMods;
    }

    public static CsServerSoftware fromName(String name) {
        if (name == null) return VANILLA;
        try {
            return valueOf(name.toUpperCase());
        } catch (Exception e) {
            return VANILLA;
        }
    }
}
