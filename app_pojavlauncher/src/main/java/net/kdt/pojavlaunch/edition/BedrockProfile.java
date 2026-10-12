package net.kdt.pojavlaunch.edition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Future Bedrock profile (Phase 2+).
 * Today Remom only launches the single official Play Store install.
 * Multi-version isolation will store one of these per user profile.
 */
public final class BedrockProfile {
    @NonNull public final String id;
    @NonNull public final String name;
    /** Mojang version label, e.g. "1.21.50" — resolved by future version manager */
    @Nullable public final String versionId;
    /** Absolute path for isolated worlds/packs when multi-version is implemented */
    @Nullable public final String isolationDir;

    public BedrockProfile(@NonNull String id, @NonNull String name,
                          @Nullable String versionId, @Nullable String isolationDir) {
        this.id = id;
        this.name = name;
        this.versionId = versionId;
        this.isolationDir = isolationDir;
    }

    /** Default profile = whatever is currently installed from Play Store. */
    public static BedrockProfile playStoreDefault() {
        return new BedrockProfile("playstore_default", "Official (Play Store)", null, null);
    }
}
