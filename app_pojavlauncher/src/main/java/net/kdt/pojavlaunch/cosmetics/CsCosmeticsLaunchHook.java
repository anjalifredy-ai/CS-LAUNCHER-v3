package net.kdt.pojavlaunch.cosmetics;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Single entry point for launch pipeline: inject local skin / cape props.
 * Call from wherever game JVM args are assembled.
 *
 * Example:
 * <pre>
 *   List&lt;String&gt; extra = CsCosmeticsLaunchHook.extraJvmArgs(context);
 *   jvmArgs.addAll(extra);
 * </pre>
 */
public final class CsCosmeticsLaunchHook {

    private CsCosmeticsLaunchHook() {}

    public static List<String> extraJvmArgs(Context context) {
        try {
            CsCosmeticsStore store = new CsCosmeticsStore(context.getApplicationContext());
            CsCosmeticEquipService equip = new CsCosmeticEquipService(store);
            return equip.buildLaunchProperties();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Absolute path of equipped imported skin, or null. */
    public static String equippedSkinPath(Context context) {
        try {
            CsCosmeticsStore store = new CsCosmeticsStore(context.getApplicationContext());
            return store.getEquippedLocalSkinPath();
        } catch (Exception e) {
            return null;
        }
    }
}
