package net.kdt.pojavlaunch.servers.connectivity;

import android.content.Context;

import net.kdt.pojavlaunch.servers.LocalServerEntry;

/**
 * Pluggable free remote connectivity for on-device Minecraft Java (TCP).
 * Local server hosting must work even when no provider is linked.
 */
public interface ConnectivityProvider {

    String id();

    String displayName();

    /** Short honest limits for the UI (free tier, agent required, etc.). */
    String limitationsSummary();

    boolean isConfigured(Context ctx);

    /**
     * Called after local server process is running.
     * May return null if only LAN is available.
     */
    ConnectivityResult onServerStarted(Context ctx, LocalServerEntry server) throws Exception;

    void onServerStopped(Context ctx, LocalServerEntry server);

    class ConnectivityResult {
        public final String publicAddress;
        public final boolean tunnelConfirmed;
        public final String statusMessage;

        public ConnectivityResult(String publicAddress, boolean tunnelConfirmed, String statusMessage) {
            this.publicAddress = publicAddress;
            this.tunnelConfirmed = tunnelConfirmed;
            this.statusMessage = statusMessage;
        }
    }
}
