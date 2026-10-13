package net.kdt.pojavlaunch.edition.host;

import android.app.Activity;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.edition.BedrockIsolationPaths;
import net.kdt.pojavlaunch.edition.BedrockVersionManager;

/** Placeholder host: inventory + extract + optional load probe; no game. */
public final class StubNativeHost implements BedrockHost {

    @NonNull
    @Override
    public Capability capability() {
        return Capability.NATIVE_MULTI_VERSION;
    }

    @NonNull
    @Override
    public String statusLine() {
        return "Host: Native multi-version (probe only — no game)";
    }

    @Override
    public boolean canLaunch(@NonNull Activity activity, @Nullable String versionId) {
        return false;
    }

    @Override
    public boolean launch(@NonNull Activity activity, @Nullable String versionId) {
        String slot = versionId == null || versionId.startsWith("Play Store")
                ? "playstore" : versionId;
        boolean hasApk = BedrockVersionManager.hasFullApk(activity, slot);
        String extractLine = "";
        if (hasApk) {
            ApkNativeInventory.scanAndSave(activity, slot);
            NativeExtractor.ExtractResult er = NativeExtractor.extractForDevice(activity, slot);
            extractLine = "\n" + er.summary();
        }
        String checklist = NativeHostChecklist.report(activity, slot);
        String iso = BedrockIsolationPaths.summary(activity, slot);
        String detected = BedrockVersionManager.getDetectedVersionName(activity, slot);

        new AlertDialog.Builder(activity)
                .setTitle("Native host not ready")
                .setMessage(
                        "Slot: " + slot + "\n" +
                        "Detected: " + (detected != null ? detected : "—") + "\n" +
                        iso + extractLine + "\n\n" +
                        checklist)
                .setPositiveButton("OK", null)
                .setNeutralButton("Open official app",
                        (d, w) -> new OfficialPlayStoreHost().launch(activity, versionId))
                .setNegativeButton(hasApk ? "Run load probe" : null,
                        hasApk ? (d, w) -> {
                            NativeLoadProbe.ProbeResult pr =
                                    NativeLoadProbe.run(activity, slot, 8);
                            new AlertDialog.Builder(activity)
                                    .setTitle("Load probe result")
                                    .setMessage(pr.summary()
                                            + "\n\nOK: " + pr.okNames
                                            + "\n\nFail (sample): "
                                            + (pr.failLines.isEmpty() ? "—"
                                            : pr.failLines.get(0))
                                            + "\n\nGame did NOT start.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } : null)
                .show();
        return true;
    }
}
