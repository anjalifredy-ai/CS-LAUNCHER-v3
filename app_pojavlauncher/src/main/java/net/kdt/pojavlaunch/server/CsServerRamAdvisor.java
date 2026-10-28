package net.kdt.pojavlaunch.server;

import android.app.ActivityManager;
import android.content.Context;

/**
 * Safe RAM recommendations for on-device Java server hosting.
 * Leaves headroom for Android, launcher, and optional client.
 */
public class CsServerRamAdvisor {

    public static class Advice {
        public final long totalDeviceMb;
        public final long availableMb;
        public final int recommendedServerMb;
        public final int minimumServerMb;
        public final int maximumSafeServerMb;
        public final String warning;

        public Advice(long totalDeviceMb, long availableMb, int recommendedServerMb,
                      int minimumServerMb, int maximumSafeServerMb, String warning) {
            this.totalDeviceMb = totalDeviceMb;
            this.availableMb = availableMb;
            this.recommendedServerMb = recommendedServerMb;
            this.minimumServerMb = minimumServerMb;
            this.maximumSafeServerMb = maximumSafeServerMb;
            this.warning = warning;
        }
    }

    private final Context context;

    public CsServerRamAdvisor(Context context) {
        this.context = context.getApplicationContext();
    }

    public Advice advise(boolean alsoPlayingClient) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(info);

        long totalMb = info.totalMem / (1024 * 1024);
        long availMb = info.availMem / (1024 * 1024);

        long reserveMb = alsoPlayingClient ? 2048L : 1536L;
        long usable = Math.max(availMb - reserveMb, 512);

        int recommended;
        if (usable >= 3072) recommended = 1536;
        else if (usable >= 2048) recommended = 1024;
        else if (usable >= 1536) recommended = 768;
        else recommended = 512;
        recommended = (int) Math.min(recommended, usable);

        int maxSafe = (int) Math.min(Math.max((long) (usable * 0.7), 512), 4096);
        int min = 512;

        String warning = null;
        if (alsoPlayingClient && totalMb < 4096) {
            warning = "Playing and hosting on the same device will compete for RAM.";
        } else if (availMb < 1024) {
            warning = "Low available memory. Close other apps before starting a server.";
        } else if (recommended < 1024) {
            warning = "Limited free RAM. Use low view-distance and few players.";
        }

        return new Advice(totalMb, availMb, recommended, min, maxSafe, warning);
    }

    public boolean isAllocationSafe(int requestedMb, boolean alsoPlayingClient) {
        Advice a = advise(alsoPlayingClient);
        return requestedMb >= a.minimumServerMb && requestedMb <= a.maximumSafeServerMb;
    }

    public boolean isAllocationSafe(int requestedMb) {
        return isAllocationSafe(requestedMb, false);
    }
}
