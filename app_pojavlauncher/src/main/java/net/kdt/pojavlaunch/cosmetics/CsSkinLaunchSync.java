package net.kdt.pojavlaunch.cosmetics;

import android.content.Context;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.PojavProfile;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Copies the equipped wardrobe local skin into the offline skin path that
 * {@link net.kdt.pojavlaunch.utils.JREUtils#launchJavaVM} already reads:
 * {@code DIR_DATA/skins/{username}_skin.png}
 *
 * Call after equip / import, and optionally right before launch.
 */
public final class CsSkinLaunchSync {

    private static final String TAG = "CsSkinLaunchSync";

    private CsSkinLaunchSync() {}

    /**
     * Sync current equipped imported skin for the active offline account.
     * @return true if a skin file was written
     */
    public static boolean syncForCurrentAccount(Context context) {
        try {
            MinecraftAccount account = PojavProfile.getCurrentProfileContent(context, null);
            if (account == null || account.username == null || account.username.isEmpty()) {
                return false;
            }
            // Only offline / non-MSA path uses local PNG override
            if (account.isMicrosoft) return false;

            String src = CsCosmeticsLaunchHook.equippedSkinPath(context);
            if (src == null) return false;
            File source = new File(src);
            if (!source.exists()) return false;

            File dir = new File(Tools.DIR_DATA, "skins");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Cannot create skins dir");
                return false;
            }
            File dest = new File(dir, account.username + "_skin.png");
            copy(source, dest);
            Log.i(TAG, "Synced wardrobe skin → " + dest.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "sync failed", e);
            return false;
        }
    }

    public static boolean syncForUsername(Context context, String username, File skinPng) {
        if (username == null || skinPng == null || !skinPng.exists()) return false;
        try {
            File dir = new File(Tools.DIR_DATA, "skins");
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, username + "_skin.png");
            copy(skinPng, dest);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "syncForUsername failed", e);
            return false;
        }
    }

    private static void copy(File src, File dest) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }
}
