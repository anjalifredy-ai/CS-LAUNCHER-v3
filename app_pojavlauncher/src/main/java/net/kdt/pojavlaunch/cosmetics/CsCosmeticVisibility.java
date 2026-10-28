package net.kdt.pojavlaunch.cosmetics;

public enum CsCosmeticVisibility {
    /** Only local player / local client */
    LOCAL_ONLY,
    /** Official Mojang/Microsoft skin/cape path */
    OFFICIAL_ACCOUNT,
    /** Requires compatible cosmetics client on both sides — not implemented yet */
    PROTOCOL_REQUIRED
}
