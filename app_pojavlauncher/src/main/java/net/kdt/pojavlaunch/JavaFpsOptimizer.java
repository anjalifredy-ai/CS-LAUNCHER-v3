package net.kdt.pojavlaunch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * Java Edition only — apply Remom max-FPS preference profile.
 * Does not touch Bedrock host code.
 */
public final class JavaFpsOptimizer {
    public static final String PREF_FLAG = "remom_java_fps_profile_v1";

    /** Safe extra JVM flags when user left javaArgs empty. */
    public static final String RECOMMENDED_JAVA_ARGS =
            "-XX:+UseG1GC " +
            "-XX:MaxGCPauseMillis=40 " +
            "-XX:G1HeapRegionSize=4M " +
            "-XX:-OmitStackTraceInFastThrow " +
            "-XX:+DisableExplicitGC";

    private JavaFpsOptimizer() {}

    /**
     * @param force if true, overwrite even if profile already applied
     */
    public static void applyMaxFpsProfile(Context ctx, boolean force) {
        if (ctx == null) return;
        SharedPreferences p = ctx.getSharedPreferences("cslauncher_settings", Context.MODE_PRIVATE);
        if (!force && p.getBoolean(PREF_FLAG, false)) return;

        int ram = recommendRamMb(ctx);
        int res = recommendResolutionPercent(ctx);

        SharedPreferences.Editor e = p.edit();
        e.putBoolean("force_vsync", false);           // unlock FPS
        e.putBoolean("vsync_in_zink", true);         // zink respects game uncapped
        e.putBoolean("sustainedPerformance", false); // peak over sustained lock
        e.putInt("allocation", ram);
        e.putInt("resolutionRatio", res);

        String existingArgs = p.getString("javaArgs", "");
        if (existingArgs == null || existingArgs.trim().isEmpty()) {
            e.putString("javaArgs", RECOMMENDED_JAVA_ARGS.trim());
        }

        e.putBoolean(PREF_FLAG, true);
        e.apply();

        Log.i("JavaFpsOptimizer",
                "Applied Max FPS profile: ram=" + ram + "MB res=" + res + "% vsync=off");
    }

    public static void applyOnceOnLoad(Context ctx) {
        applyMaxFpsProfile(ctx, false);
    }

    public static int recommendRamMb(Context ctx) {
        int deviceRam = Tools.getTotalDeviceMemory(ctx);
        boolean bits32 = Architecture.is32BitsDevice();
        if (deviceRam < 1024) return 296;
        if (deviceRam < 1536) return 448;
        if (deviceRam < 2048) return 656;
        if (bits32) return 696;
        if (deviceRam < 3064) return 1024;
        if (deviceRam < 4096) return 1280;
        if (deviceRam < 6144) return 1792;
        if (deviceRam < 8192) return 2560;
        // Cap so Android + JVM both breathe
        return Math.min(4096, deviceRam / 2);
    }

    public static int recommendResolutionPercent(Context ctx) {
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        int minSide = Math.min(metrics.widthPixels, metrics.heightPixels);
        // Prefer 720p class internal res for FPS; powerful high-end can use 900p-ish
        int deviceRam = Tools.getTotalDeviceMemory(ctx);
        int targetSide = deviceRam >= 6144 ? 900 : 720;
        if (minSide <= targetSide) return 100;
        float ratio = 100f * targetSide / minSide;
        int stepped = (int) (Math.ceil(ratio / 5f) * 5f);
        return Math.max(25, Math.min(100, stepped));
    }

    public static String statusLine(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences("cslauncher_settings", Context.MODE_PRIVATE);
        boolean applied = p.getBoolean(PREF_FLAG, false);
        boolean vsync = p.getBoolean("force_vsync", false);
        int ram = p.getInt("allocation", recommendRamMb(ctx));
        int res = p.getInt("resolutionRatio", recommendResolutionPercent(ctx));
        return "Java FPS profile " + (applied ? "ON" : "off")
                + " | VSync force=" + vsync
                + " | RAM=" + ram + "MB"
                + " | Res=" + res + "%"
                + " | API=" + Build.VERSION.SDK_INT;
    }
}
