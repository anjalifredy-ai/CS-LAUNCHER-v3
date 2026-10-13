package net.kdt.pojavlaunch.edition.host;

import android.app.Activity;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.edition.BedrockIsolationPaths;
import net.kdt.pojavlaunch.edition.BedrockVersionManager;
import net.kdt.pojavlaunch.edition.EditionMode;

/**
 * Step 4/5: placeholder for Levi-style in-process host.
 * Has isolation + APK path; does NOT load natives yet.
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
        return "Host: Native multi-version (stub — not playable yet)";
    }

    @Override
    public boolean canLaunch(@NonNull Activity activity, @Nullable String versionId) {
        // Never claim ready until real native implemented
        return false;
    }

    @Override
    public boolean launch(@NonNull Activity activity, @Nullable String versionId) {
        String slot = versionId == null || versionId.startsWith("Play Store")
                ? "playstore" : versionId;
        boolean hasApk = BedrockVersionManager.hasFullApk(activity, slot);
        String iso = BedrockIsolationPaths.summary(activity, slot);
        String detected = BedrockVersionManager.getDetectedVersionName(activity, slot);

        new AlertDialog.Builder(activity)
                .setTitle("Native host not ready")
                .setMessage(
                        "Levi-style in-app host is still a stub.\n\n" +
                        "Slot: " + slot + "\n" +
                        "Detected: " + (detected != null ? detected : "—") + "\n" +
                        "APK imported: " + (hasApk ? "yes" : "no") + "\n" +
                        iso + "\n\n" +
                        "Play will use Official Play Store host instead if available.\n" +
                        "Loading .so from imported APK = future work.")
                .setPositiveButton("OK", null)
                .setNeutralButton("Open official app",
                        (d, w) -> new OfficialPlayStoreHost().launch(activity, versionId))
                .show();
        return true;
    }
}
