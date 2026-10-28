package net.kdt.pojavlaunch.server;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Resolves LAN IPv4 for on-device server so other devices can connect.
 */
public class CsLanAddressHelper {

    public static class LanInfo {
        public final String ipv4;
        public final int port;
        public final String joinHint;

        public LanInfo(String ipv4, int port) {
            this.ipv4 = ipv4;
            this.port = port;
            this.joinHint = (ipv4 != null ? ipv4 : "?") + ":" + port;
        }
    }

    private final Context context;

    public CsLanAddressHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public LanInfo currentLanInfo(int port) {
        String ip = resolveWifiIp();
        if (ip == null) ip = resolveFirstIpv4();
        return new LanInfo(ip, port);
    }

    @SuppressWarnings("deprecation")
    private String resolveWifiIp() {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return null;
            int ip = wm.getConnectionInfo().getIpAddress();
            if (ip == 0) return null;
            return Formatter.formatIpAddress(ip);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveFirstIpv4() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(ifaces)) {
                if (!iface.isUp() || iface.isLoopback()) continue;
                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
