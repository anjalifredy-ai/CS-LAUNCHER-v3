package net.kdt.pojavlaunch.edition.host;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.edition.BedrockProfile;
import net.kdt.pojavlaunch.edition.BedrockProfileStore;
import net.kdt.pojavlaunch.edition.BedrockVersionManager;
import net.kdt.pojavlaunch.edition.EditionMode;

/**
 * Step 5/5: choose host for Play.
 * Prefer official app (works). If user selected a non-playstore imported slot,
 * show stub native honesty dialog, then allow fallback to official.
 */
public final class BedrockHostRouter {
    private static final OfficialPlayStoreHost OFFICIAL = new OfficialPlayStoreHost();
    private static final StubNativeHost NATIVE_STUB = new StubNativeHost();

    private BedrockHostRouter() {}

    @NonNull
    public static BedrockHost resolve(@Nullable String versionId) {
        String slot = normalize(versionId);
        // Only playstore / null → official path as primary
        if ("playstore".equals(slot)) {
            return OFFICIAL;
        }
        // Imported / catalog slot → stub native (honest); can still fallback
        return NATIVE_STUB;
    }

    public static boolean launchFromProfile(@NonNull Activity activity) {
        if (!EditionMode.isBedrock(activity)) return false;
        BedrockProfile sel = BedrockProfileStore.getSelected(activity);
        String versionId = sel.versionId;
        String slot = normalize(versionId);

        BedrockHost host = resolve(versionId);

        // If stub and official is installed, for playstore-like still prefer official
        if (host.capability() == BedrockHost.Capability.NATIVE_MULTI_VERSION) {
            boolean hasApk = BedrockVersionManager.hasFullApk(activity, slot);
            // User asked multi-version: show stub status, offer official
            if (!hasApk && EditionMode.isOfficialBedrockInstalled(activity)) {
                // No import — just open official
                return OFFICIAL.launch(activity, versionId);
            }
            return host.launch(activity, versionId);
        }
        return host.launch(activity, versionId);
    }

    @NonNull
    public static String uiStatus(@Nullable String versionId) {
        return resolve(versionId).statusLine();
    }

    private static String normalize(@Nullable String versionId) {
        if (versionId == null || versionId.startsWith("Play Store")) return "playstore";
        return versionId;
    }
}
