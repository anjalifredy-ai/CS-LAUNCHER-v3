package net.kdt.pojavlaunch.edition;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Step 1/5: world & pack paths under version isolation. */
public final class BedrockIsolationPaths {
    private BedrockIsolationPaths() {}

    public static File worldsDir(Context c, String versionId) {
        File f = new File(BedrockVersionManager.isolationDir(c, versionId),
                "games/com.mojang/minecraftWorlds");
        //noinspection ResultOfMethodCallIgnored
        f.mkdirs();
        return f;
    }

    public static File resourcePacksDir(Context c, String versionId) {
        File f = new File(BedrockVersionManager.isolationDir(c, versionId),
                "games/com.mojang/resource_packs");
        //noinspection ResultOfMethodCallIgnored
        f.mkdirs();
        return f;
    }

    public static File behaviorPacksDir(Context c, String versionId) {
        File f = new File(BedrockVersionManager.isolationDir(c, versionId),
                "games/com.mojang/behavior_packs");
        //noinspection ResultOfMethodCallIgnored
        f.mkdirs();
        return f;
    }

    public static File metaDir(Context c, String versionId) {
        File f = new File(BedrockVersionManager.isolationDir(c, versionId), "meta");
        //noinspection ResultOfMethodCallIgnored
        f.mkdirs();
        return f;
    }

    /** Ensure full tree for a version slot. */
    public static File ensureAll(Context c, String versionId) {
        File root = BedrockVersionManager.isolationDir(c, versionId);
        worldsDir(c, versionId);
        resourcePacksDir(c, versionId);
        behaviorPacksDir(c, versionId);
        metaDir(c, versionId);
        return root;
    }

    public static List<String> listWorldFolderNames(Context c, String versionId) {
        List<String> out = new ArrayList<>();
        File[] files = worldsDir(c, versionId).listFiles();
        if (files == null) return out;
        for (File f : files) {
            if (f.isDirectory()) out.add(f.getName());
        }
        return out;
    }

    public static String summary(Context c, String versionId) {
        int w = listWorldFolderNames(c, versionId).size();
        boolean apk = BedrockVersionManager.hasFullApk(c, versionId);
        return "worlds=" + w + " apk=" + (apk ? "yes" : "no")
                + " root=" + BedrockVersionManager.isolationDir(c, versionId).getAbsolutePath();
    }
}
