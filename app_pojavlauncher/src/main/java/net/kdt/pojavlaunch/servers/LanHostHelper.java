package net.kdt.pojavlaunch.servers;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Same-WiFi multiplayer helper — shows host LAN IP so friends can join
 * Open-to-LAN worlds without a dedicated server.
 */
public final class LanHostHelper {
    private LanHostHelper() {}

    public static String getLanIpv4(Context ctx) {
        String wifi = wifiIp(ctx);
        if (wifi != null && !wifi.isEmpty() && !"0.0.0.0".equals(wifi))
            return wifi;
        String iface = interfaceIp();
        if (iface != null) return iface;
        return null;
    }

    @SuppressWarnings("deprecation")
    private static String wifiIp(Context ctx) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return null;
            int ip = wm.getConnectionInfo().getIpAddress();
            if (ip == 0) return null;
            return Formatter.formatIpAddress(ip);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String interfaceIp() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            if (en == null) return null;
            for (NetworkInterface ni : Collections.list(en)) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                String name = ni.getName() != null ? ni.getName().toLowerCase() : "";
                if (!(name.contains("wlan") || name.contains("wifi") || name.contains("eth") || name.contains("ap")))
                    continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static String formatJoinHint(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "Wi‑Fi IP nahi mila — phone Wi‑Fi pe raho, Refresh dabao.";
        }
        return ip + "  (port Open-to-LAN screen pe dikhega)";
    }
}
