package net.kdt.pojavlaunch.servers;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Modern Server List Ping (1.7+). Research-backed, zero-dependency.
 * Handshake (0x00) + Status Request (0x00) → JSON with version, players, description, favicon.
 * Runs on background thread; caller handles UI posting.
 */
public class ServerPinger {
    private static final String TAG = "ServerPinger";
    private static final int TIMEOUT_MS = 4200;
    private static final int PROTOCOL = 760; // 1.19.4, works for most servers (handshake protocol)

    public interface Callback {
        void onResult(ServerEntry entry);
        void onError(ServerEntry entry, Exception e);
    }

    public static void pingAsync(ServerEntry entry, Callback cb) {
        new Thread(() -> {
            try {
                ping(entry);
                if (cb != null) cb.onResult(entry);
            } catch (Exception e) {
                Log.w(TAG, "ping failed " + entry.address, e);
                entry.online = false; entry.pingMs = -1; entry.lastPingEpoch = System.currentTimeMillis();
                if (cb != null) cb.onError(entry, e);
            }
        }, "ServerPinger-" + entry.address).start();
    }

    public static void ping(ServerEntry entry) throws Exception {
        String host = entry.getHost();
        // strip [] for ipv6
        String connectHost = host;
        if (connectHost.startsWith("[") && connectHost.endsWith("]")) connectHost = connectHost.substring(1, connectHost.length()-1);
        int port = entry.getPort();
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            socket.connect(new InetSocketAddress(connectHost, port), TIMEOUT_MS);
            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                // handshake
                ByteArrayOutputStream hsBuf = new ByteArrayOutputStream();
                DataOutputStream hs = new DataOutputStream(hsBuf);
                writeVarInt(hs, 0x00);
                writeVarInt(hs, PROTOCOL);
                writeString(hs, connectHost);
                hs.writeShort(port);
                writeVarInt(hs, 1); // status
                byte[] hsBytes = hsBuf.toByteArray();
                writeVarInt(out, hsBytes.length);
                out.write(hsBytes);

                // status request
                writeVarInt(out, 1); // length
                writeVarInt(out, 0x00);
                out.flush();

                // read response
                int len = readVarInt(in);
                int packetId = readVarInt(in);
                if (packetId != 0x00) throw new RuntimeException("Invalid packet " + packetId);
                int jsonLen = readVarInt(in);
                byte[] jsonBytes = new byte[jsonLen];
                in.readFully(jsonBytes);
                String jsonStr = new String(jsonBytes, StandardCharsets.UTF_8);
                long ping = System.currentTimeMillis() - start;
                parseJson(entry, jsonStr, ping);

                // ping/pong for latency (optional, we already have tcp time)
                try {
                    long now = System.currentTimeMillis();
                    writeVarInt(out, 9);
                    writeVarInt(out, 0x01);
                    out.writeLong(now);
                    out.flush();
                    // read pong
                    int pongLen = readVarInt(in);
                    int pongId = readVarInt(in);
                    if (pongId == 0x01) {
                        long sent = in.readLong();
                        entry.pingMs = System.currentTimeMillis() - sent;
                    }
                } catch (Exception ignored) { if (entry.pingMs < 0) entry.pingMs = ping; }
            }
        }
        entry.lastPingEpoch = System.currentTimeMillis();
    }

    private static void parseJson(ServerEntry e, String json, long fallbackPing) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject version = root.optJSONObject("version");
            if (version != null) {
                e.versionName = version.optString("name", "");
                e.protocol = version.optInt("protocol", -1);
            }
            JSONObject players = root.optJSONObject("players");
            if (players != null) {
                e.playersOnline = players.optInt("online", -1);
                e.playersMax = players.optInt("max", -1);
            }
            // description can be string or object
            Object desc = root.opt("description");
            if (desc instanceof String) {
                e.motd = (String) desc;
                e.motdRaw = (String) desc;
            } else if (desc instanceof JSONObject) {
                e.motd = ((JSONObject) desc).optString("text", "");
                // try extra
                if (e.motd.isEmpty() && ((JSONObject) desc).has("extra")) {
                    JSONArray extra = ((JSONObject) desc).optJSONArray("extra");
                    if (extra != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i=0;i<extra.length();i++) sb.append(extra.optJSONObject(i).optString("text",""));
                        e.motd = sb.toString();
                    }
                }
                e.motdRaw = desc.toString();
            }
            if (e.motd != null) {
                // strip color codes §
                e.motd = e.motd.replaceAll("§.", "");
                // trim
                e.motd = e.motd.trim();
                if (e.name == null || e.name.equals(e.address) || e.name.isEmpty()) {
                    // derive name from MOTD first line
                    String[] lines = e.motd.split("\n");
                    if (lines.length>0 && !lines[0].trim().isEmpty()) e.name = lines[0].trim();
                }
            }
            String favicon = root.optString("favicon", null);
            if (favicon != null && favicon.startsWith("data:image")) e.iconBase64 = favicon;
            e.online = true;
            if (e.pingMs < 0) e.pingMs = fallbackPing;
        } catch (Exception ex) {
            Log.w(TAG, "parse fail", ex);
            e.online = true; // we got json, consider online
            e.pingMs = fallbackPing;
        }
    }

    private static void writeVarInt(DataOutputStream out, int v) throws Exception {
        while ((v & 0xFFFFFF80) != 0) { out.writeByte((v & 0x7F) | 0x80); v >>>= 7; }
        out.writeByte(v);
    }
    private static int readVarInt(DataInputStream in) throws Exception {
        int numRead=0, result=0; byte read;
        do { read=in.readByte(); int val=(read & 0x7F); result|=val<<(7*numRead); numRead++; if(numRead>5) throw new RuntimeException("VarInt too big"); } while((read & 0x80)!=0);
        return result;
    }
    private static void writeString(DataOutputStream out, String s) throws Exception {
        byte[] b=s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out,b.length);
        out.write(b);
    }
}
