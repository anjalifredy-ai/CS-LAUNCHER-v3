package net.kdt.pojavlaunch.cosmetics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies equipped loadout to launch args / local skin path.
 * Skins: inject local PNG path for offline/local use.
 * Other cosmetics: metadata only until protocol exists.
 */
public class CsCosmeticEquipService {

    private final CsCosmeticsStore store;

    public CsCosmeticEquipService(CsCosmeticsStore store) {
        this.store = store;
    }

    /**
     * Returns JVM / game args fragment for local skin override.
     * Example: -Dcs.localSkin=/path/to/skin.png
     */
    public List<String> buildLaunchProperties() {
        List<String> props = new ArrayList<>();
        String skinPath = store.getEquippedLocalSkinPath();
        if (skinPath != null && new File(skinPath).exists()) {
            props.add("-Dcs.localSkin=" + skinPath);
            // Also common offline skin property used by some forks
            props.add("-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=false");
        }
        CsEquippedLoadout loadout = store.getLoadout();
        if (loadout.capeId != null) {
            props.add("-Dcs.equippedCape=" + loadout.capeId);
        }
        return props;
    }

    public String getLocalSkinAbsolutePath() {
        return store.getEquippedLocalSkinPath();
    }
}
