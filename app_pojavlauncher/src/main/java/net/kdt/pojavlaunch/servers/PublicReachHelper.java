package net.kdt.pojavlaunch.servers;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Detects public IPv4 and explains how distant players can join.
 *
 * Truth:
 * - Same Wi‑Fi → LAN IP works.
 * - Far away → need playit.gg / ngrok / router port-forward OR cloud host.
 * Mobile CGNAT almost never allows direct port-forward; tunnel is required.
 */
public final class PublicReachHelper {

    private static final String TAG = "PublicReach";

    public static class ReachInfo {
        public String lanIp;
        public String publicIp;
        public String joinHint;
        public boolean likelyNeedsTunnel;
    }

    private PublicReachHelper() {}

    public static ReachInfo probe(Context ctx, int port) {
        ReachInfo info = new ReachInfo();
        info.lanIp = LanHostHelper.getLanIpv4(ctx);
        info.publicIp = fetchPublicIp();

        boolean privateLan = info.lanIp != null && (
                info.lanIp.startsWith("192.168.") ||
                info.lanIp.startsWith("10.") ||
                info.lanIp.startsWith("172."));

        info.likelyNeedsTunnel = privateLan;

        StringBuilder sb = new StringBuilder();
        sb.append("Solo (same phone): 127.0.0.1:").append(port).append('\n');
        if (info.lanIp != null) {
            sb.append("Same Wi‑Fi friends: ").append(info.lanIp).append(':').append(port).append('\n');
        }
        if (info.publicIp != null) {
            sb.append("Public IP (needs port-forward on router): ")
                    .append(info.publicIp).append(':').append(port).append('\n');
        }
        sb.append("Door / mobile data friends: use playit.gg tunnel → Save public address in Server Hub.");
        info.joinHint = sb.toString();
        return info;
    }

    public static String fetchPublicIp() {
        String[] endpoints = {
                "https://api.ipify.org",
                "https://ifconfig.me/ip",
                "https://icanhazip.com"
        };
        for (String ep : endpoints) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(ep).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("User-Agent", "CS-Launcher/3");
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                    String ip = br.readLine();
                    if (ip != null) {
                        ip = ip.trim();
                        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) return ip;
                    }
                }
            } catch (Throwable t) {
                Log.d(TAG, "endpoint failed " + ep);
            }
        }
        return null;
    }
}
