package net.kdt.pojavlaunch.edition.host;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Step 2/5: Levi-style host contract.
 * Implementations: official Play Store launch, future native multi-version.
 */
public interface BedrockHost {

    enum Capability {
        /** Opens installed com.mojang.minecraftpe */
        OFFICIAL_APP,
        /** Runs imported APK inside Remom (not implemented yet) */
        NATIVE_MULTI_VERSION
    }

    @NonNull Capability capability();

    /** Human-readable status for UI. */
    @NonNull String statusLine();

    /** Whether launch is allowed right now. */
    boolean canLaunch(@NonNull Activity activity, @Nullable String versionId);

    /**
     * Start game.
     * @return true if request was handled (success or shown error UI)
     */
    boolean launch(@NonNull Activity activity, @Nullable String versionId);
}
