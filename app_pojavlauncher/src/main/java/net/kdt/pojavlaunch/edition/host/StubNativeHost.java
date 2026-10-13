package net.kdt.pojavlaunch.edition.host;

import android.app.Activity;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.edition.BedrockIsolationPaths;
import net.kdt.pojavlaunch.edition.BedrockVersionManager;

/**
 * Placeholder for Levi-style in-process host.
 * Shows inventory + checklist; does NOT load natives.
 */
public final class StubNativeHost implements BedrockHost {

    @NonNull
    @Override
    public Capability capability() {
        return Capability.NATIVE_MULTI_VERSION;
    }

    @NonNull
    @Override
    public String statusLine() {
        return "Host: Native multi-version (stub — inventory only)";
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
        if (hasApk) {
            ApkNativeInventory.scanAndSave(activity, slot);
        }
        String checklist = NativeHostChecklist.report(activity, slot);
        String iso = BedrockIsolationPaths.summary(activity, slot);
        String detected = BedrockVersionManager.getDetectedVersionName(activity, slot);

        new AlertDialog.Builder(activity)
                .setTitle("Native host not ready")
                .setMessage(
                        "Slot: " + slot + "\n" +
                        "Detected: " + (detected != null ? detected : "—") + "\n" +
                        iso + "\n\n" +
                        checklist)
                .setPositiveButton("OK", null)
                .setNeutralButton("Open official app",
                        (d, w) -> new OfficialPlayStoreHost().launch(activity, versionId))
                .show();
        return true;
    }
}
