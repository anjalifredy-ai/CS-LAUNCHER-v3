package net.kdt.pojavlaunch.servers.connectivity;

import android.content.Context;

import net.kdt.pojavlaunch.servers.LanHostHelper;
import net.kdt.pojavlaunch.servers.LocalServerEntry;
import net.kdt.pojavlaunch.servers.PublicReachHelper;

/**
 * Uses LAN IP always; exposes public IP:port only as a candidate when the
 * network might allow inbound TCP (rare on mobile CGNAT). Never claims
 * remote join works without a real connection test.
 */
public final class DirectNetworkProvider implements ConnectivityProvider {

    public static final String ID = "direct_network";

    @Override public String id() { return ID; }

    @Override public String displayName() { return "LAN / direct IP"; }

    @Override
    public String limitationsSummary() {
        return "Same Wi‑Fi: use LAN IP. Mobile data / CGNAT almost never accepts inbound Minecraft TCP. "
                + "Public IP is shown only as a candidate — port-forward or tunnel usually still required.";
    }

    @Override
    public boolean isConfigured(Context ctx) {
        return true;
    }

    @Override
    public ConnectivityResult onServerStarted(Context ctx, LocalServerEntry server) {
        String lan = LanHostHelper.getLanIpv4(ctx);
        String lanAddr = server.displayAddress(lan);
        PublicReachHelper.ReachInfo info = PublicReachHelper.probe(ctx, server.port);
        if (info.publicIp != null && !info.likelyNeedsTunnel) {
            String candidate = info.publicIp + ":" + server.port;
            return new ConnectivityResult(candidate, false,
                    "Candidate public " + candidate + " — verify with a remote device; many ISPs block inbound.");
        }
        return new ConnectivityResult(null, false,
                "LAN: " + lanAddr + (info.publicIp != null
                        ? " · Public IP seen " + info.publicIp + " but tunnel likely required (CGNAT/NAT)."
                        : " · No reliable public inbound path detected."));
    }

    @Override
    public void onServerStopped(Context ctx, LocalServerEntry server) {}
}
