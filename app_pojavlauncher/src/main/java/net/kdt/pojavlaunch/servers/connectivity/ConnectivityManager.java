package net.kdt.pojavlaunch.servers.connectivity;

import android.content.Context;
import android.util.Log;

import net.kdt.pojavlaunch.servers.LocalServerEntry;
import net.kdt.pojavlaunch.servers.LocalServerStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs configured connectivity providers after local server start.
 * Local process is independent of tunnel success.
 */
public final class ConnectivityManager {
    private static final String TAG = "CsConnectivity";

    private static final List<ConnectivityProvider> PROVIDERS = new ArrayList<>();

    static {
        PROVIDERS.add(new ManualTunnelProvider());
        PROVIDERS.add(new DirectNetworkProvider());
    }

    private ConnectivityManager() {}

    public static List<ConnectivityProvider> providers() {
        return new ArrayList<>(PROVIDERS);
    }

    public static String combinedLimitations() {
        StringBuilder sb = new StringBuilder();
        for (ConnectivityProvider p : PROVIDERS) {
            sb.append("• ").append(p.displayName()).append(": ").append(p.limitationsSummary()).append('\n');
        }
        return sb.toString().trim();
    }

    /**
     * Apply providers after Minecraft server process is up.
     * Prefer manual tunnel address when configured.
     */
    public static ConnectivityProvider.ConnectivityResult afterServerStart(Context ctx, LocalServerEntry server) {
        ConnectivityProvider.ConnectivityResult best = null;
        for (ConnectivityProvider p : PROVIDERS) {
            try {
                ConnectivityProvider.ConnectivityResult r = p.onServerStarted(ctx, server);
                if (r == null) continue;
                if (r.publicAddress != null && !r.publicAddress.isEmpty()) {
                    server.publicAddress = r.publicAddress;
                    LocalServerStore.update(ctx, server);
                    best = r;
                    if (ManualTunnelProvider.ID.equals(p.id())) break;
                } else if (best == null) {
                    best = r;
                }
            } catch (Throwable t) {
                Log.w(TAG, "provider " + p.id(), t);
            }
        }
        if (best == null) {
            best = new ConnectivityProvider.ConnectivityResult(null, false, "Local server running. Remote path not configured.");
        }
        return best;
    }

    public static void afterServerStop(Context ctx, LocalServerEntry server) {
        for (ConnectivityProvider p : PROVIDERS) {
            try { p.onServerStopped(ctx, server); } catch (Throwable ignored) {}
        }
    }
}
