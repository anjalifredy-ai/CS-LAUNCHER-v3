package net.kdt.pojavlaunch.edition.host;

import android.content.Context;

import net.kdt.pojavlaunch.edition.BedrockVersionManager;
import net.kdt.pojavlaunch.edition.EditionMode;

/** Honest readiness checklist for future native host. */
public final class NativeHostChecklist {
    private NativeHostChecklist() {}

    public static String report(Context c, String versionId) {
        String slot = versionId == null || versionId.startsWith("Play Store")
                ? "playstore" : versionId;
        boolean owned = EditionMode.isOfficialBedrockInstalled(c);
        boolean apk = BedrockVersionManager.hasFullApk(c, slot);
        boolean inv = apk;
        boolean extract = NativeExtractor.hasExtracted(c, slot);
        boolean probed = NativeLoadProbe.hasProbeLog(c, slot);
        // Partial System.load of random libs is NOT "libs loaded for game"
        boolean loadSo = false;
        boolean gameWindow = false;

        String invLine = apk ? ApkNativeInventory.loadSummary(c, slot) : "—";
        String extLine = extract
                ? ("yes count=" + NativeExtractor.countExtracted(c, slot))
                : "no";
        String probeLine = probed ? NativeLoadProbe.lastSummary(c, slot) : "not run";

        return "Native host checklist\n"
                + "[" + (owned ? "x" : " ") + "] Official Minecraft owned/installed\n"
                + "[" + (apk ? "x" : " ") + "] Official APK imported for slot\n"
                + "[" + (inv ? "x" : " ") + "] .so inventory scanned (" + invLine + ")\n"
                + "[" + (extract ? "x" : " ") + "] Natives extracted (" + extLine + ")\n"
                + "[" + (probed ? "x" : " ") + "] Load probe logged (" + probeLine + ")\n"
                + "[" + (loadSo ? "x" : " ") + "] Native stack ready for game (never from probe alone)\n"
                + "[" + (gameWindow ? "x" : " ") + "] In-app game window\n"
                + "\nSee docs/NATIVE_LOAD_BARRIERS.md";
    }
}
