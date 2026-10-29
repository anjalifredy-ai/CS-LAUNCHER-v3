package net.kdt.pojavlaunch.servers.connectivity;

import android.content.Context;

import net.kdt.pojavlaunch.servers.LocalServerEntry;
import net.kdt.pojavlaunch.servers.PlayitAccountStore;

/**
 * Free remote path: user links a TCP tunnel (e.g. playit) once and pastes the
 * public Minecraft address. No secrets hardcoded in the APK.
 *
 * Official playit PC agent is not assumed to run inside this process.
 * On Android-only devices, the agent may run via Termux (community) or the
 * user pastes an address from any TCP tunnel that targets 127.0.0.1:port.
 */
public final class ManualTunnelProvider implements ConnectivityProvider {

    public static final String ID = "manual_tcp_tunnel";

    @Override public String id() { return ID; }

    @Override public String displayName() { return "Free TCP tunnel (playit / manual)"; }

    @Override
    public String limitationsSummary() {
        return "Free remote join needs a live TCP tunnel to this phone's server port. "
                + "playit.gg free tier works; agent must stay online (PC, Termux, or future in-app agent). "
                + "Not unlimited; provider limits apply. Local hosting works without a tunnel.";
    }

    @Override
    public boolean isConfigured(Context ctx) {
        String a = PlayitAccountStore.getDefaultPublicAddress(ctx);
        return a != null && !a.trim().isEmpty();
    }

    @Override
    public ConnectivityResult onServerStarted(Context ctx, LocalServerEntry server) {
        PlayitAccountStore.applyDefaultToServer(ctx, server);
        String addr = server.publicAddress;
        if (addr == null || addr.trim().isEmpty()) {
            addr = PlayitAccountStore.getDefaultPublicAddress(ctx);
        }
        if (addr == null || addr.trim().isEmpty()) {
            return new ConnectivityResult(null, false,
                    "Local only. Reach tab → paste free TCP tunnel address for remote friends.");
        }
        return new ConnectivityResult(addr.trim(), false,
                "Public address saved. Confirm tunnel agent is running and points to 127.0.0.1:"
                        + server.port + " before remote join is reliable.");
    }

    @Override
    public void onServerStopped(Context ctx, LocalServerEntry server) {
    }
}
