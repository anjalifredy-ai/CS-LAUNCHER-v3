package net.kdt.pojavlaunch.edition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Day 5: profile bound to version slot + isolation + optional APK path. */
public final class BedrockProfile {
    @NonNull public final String id;
    @NonNull public final String name;
    /** Version slot id, e.g. playstore / 1.21.50 */
    @Nullable public final String versionId;
    /** Isolation root for worlds/packs */
    @Nullable public final String isolationDir;
    /** Absolute path to imported official.apk, or null */
    @Nullable public final String apkPath;

    public BedrockProfile(@NonNull String id, @NonNull String name,
                          @Nullable String versionId, @Nullable String isolationDir) {
        this(id, name, versionId, isolationDir, null);
    }

    public BedrockProfile(@NonNull String id, @NonNull String name,
                          @Nullable String versionId, @Nullable String isolationDir,
                          @Nullable String apkPath) {
        this.id = id;
        this.name = name;
        this.versionId = versionId;
        this.isolationDir = isolationDir;
        this.apkPath = apkPath;
    }

    public static BedrockProfile playStoreDefault() {
        return new BedrockProfile("playstore_default", "Official (Play Store)",
                "Play Store (current)", null, null);
    }
}
