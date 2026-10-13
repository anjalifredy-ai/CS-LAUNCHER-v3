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
        boolean loadSo = false;
        boolean gameWindow = false;

        String invLine = apk ? ApkNativeInventory.loadSummary(c, slot) : "—";
        String extLine = extract
                ? ("yes count=" + NativeExtractor.countExtracted(c, slot))
                : "no";

        return "Native host checklist\n"
                + "[" + (owned ? "x" : " ") + "] Official Minecraft owned/installed\n"
                + "[" + (apk ? "x" : " ") + "] Official APK imported for slot\n"
                + "[" + (inv ? "x" : " ") + "] .so inventory scanned (" + invLine + ")\n"
                + "[" + (extract ? "x" : " ") + "] Natives extracted (" + extLine + ")\n"
                + "[" + (loadSo ? "x" : " ") + "] Native libs loaded in process\n"
                + "[" + (gameWindow ? "x" : " ") + "] In-app game window\n"
                + "\nPlayable multi-version only when last two are [x].";
    }
}
