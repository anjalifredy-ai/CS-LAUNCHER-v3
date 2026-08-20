package net.kdt.pojavlaunch.servers;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServerPinger {
    private static final String TAG = "ServerPinger";
    private static final int TIMEOUT_MS = 4500;
    private static final int PROTOCOL = 760;
    public interface Callback { void onResult(ServerEntry entry); void onError(ServerEntry entry, Exception e); }
    public static void pingAsync(ServerEntry entry, Callback cb) {
        new Thread(() -> {
            try { ping(entry); if (cb != null) cb.onResult(entry); }
            catch (Exception e) { Log.w(TAG, "ping failed " + entry.address, e); entry.online=false; entry.pingMs=-1; entry.lastPingEpoch=System.currentTimeMillis(); if (entry.motd==null||entry.motd.isEmpty()) entry.motd="Offline — tap refresh"; if(cb!=null) cb.onError(entry,e); }
        }, "ServerPinger-" + entry.address).start();
    }
    public static void ping(ServerEntry entry) throws Exception {
        String host = entry.getHost();
        String connectHost = host;
        if (connectHost.startsWith("[") && connectHost.endsWith("]")) connectHost = connectHost.substring(1, connectHost.length()-1);
        int port = entry.getPort();
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            socket.connect(new InetSocketAddress(connectHost, port), TIMEOUT_MS);
            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {
                ByteArrayOutputStream hsBuf = new ByteArrayOutputStream();
                DataOutputStream hs = new DataOutputStream(hsBuf);
                writeVarInt(hs, 0x00);
                writeVarInt(hs, PROTOCOL);
                writeString(hs, connectHost);
                hs.writeShort(port);
                writeVarInt(hs, 1);
                byte[] hsBytes = hsBuf.toByteArray();
                writeVarInt(out, hsBytes.length);
                out.write(hsBytes);
                writeVarInt(out, 1);
                writeVarInt(out, 0x00);
                out.flush();
                int len = readVarInt(in);
                int packetId = readVarInt(in);
                if (packetId != 0x00) throw new RuntimeException("Invalid packet " + packetId);
                int jsonLen = readVarInt(in);
                byte[] jsonBytes = new byte[jsonLen];
                in.readFully(jsonBytes);
                String jsonStr = new String(jsonBytes, StandardCharsets.UTF_8);
                long ping = System.currentTimeMillis() - start;
                parseJson(entry, jsonStr, ping);
                try {
                    long now = System.currentTimeMillis();
                    writeVarInt(out, 9);
                    writeVarInt(out, 0x01);
                    out.writeLong(now);
                    out.flush();
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
        if (entry.pingMs < 0) entry.pingMs = System.currentTimeMillis() - start;
    }
    private static void parseJson(ServerEntry e, String json, long fallbackPing) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject version = root.optJSONObject("version");
            if (version != null) { e.versionName = version.optString("name",""); e.protocol = version.optInt("protocol",-1); }
            JSONObject players = root.optJSONObject("players");
            if (players != null) { e.playersOnline = players.optInt("online",-1); e.playersMax = players.optInt("max",-1); }
            Object desc = root.opt("description");
            String motd = "";
            String raw = "";
            if (desc instanceof String) {
                motd = (String)desc; raw = motd;
            } else if (desc instanceof JSONObject) {
                JSONObject jo = (JSONObject)desc;
                motd = jo.optString("text","");
                if (motd.isEmpty() && jo.has("extra")) {
                    JSONArray extra = jo.optJSONArray("extra");
                    if (extra != null) {
                        StringBuilder sb=new StringBuilder();
                        for(int i=0;i<extra.length();i++){
                            JSONObject o = extra.optJSONObject(i);
                            if(o!=null) sb.append(o.optString("text",""));
                        }
                        motd = sb.toString();
                    }
                }
                // Handle translate with with
                if (motd.isEmpty() && jo.has("translate")) {
                    motd = jo.optString("translate","");
                    JSONArray with = jo.optJSONArray("with");
                    if (with!=null && with.length()>0) {
                        StringBuilder sb=new StringBuilder(motd);
                        for(int i=0;i<with.length();i++){
                            Object w = with.opt(i);
                            if(w instanceof JSONObject) sb.append(" ").append(((JSONObject)w).optString("text",""));
                            else if(w instanceof String) sb.append(" ").append((String)w);
                        }
                        motd = sb.toString();
                    }
                }
                raw = jo.toString();
            } else if (desc instanceof JSONArray) {
                JSONArray arr = (JSONArray)desc;
                StringBuilder sb=new StringBuilder();
                for(int i=0;i<arr.length();i++){
                    Object o=arr.opt(i);
                    if(o instanceof JSONObject) sb.append(((JSONObject)o).optString("text",""));
                    else if(o instanceof String) sb.append((String)o);
                }
                motd = sb.toString();
                raw = arr.toString();
            }
            if (motd==null) motd="";
            // Strip section codes and trim, keep newlines for 2-line MOTD
            motd = motd.replaceAll("§.", "").trim();
            // Minecraft MOTD often has two lines separated by \n — keep as is
            if (motd.isEmpty() && raw != null && !raw.isEmpty()) motd = raw.replaceAll("§.", "").trim();
            if (motd.isEmpty()) motd = "A Minecraft Server";
            e.motd = motd;
            e.motdRaw = raw != null && !raw.isEmpty() ? raw : motd;
            if (e.name == null || e.name.equals(e.address) || e.name.isEmpty()) {
                String[] lines = motd.split("\n");
                String first = lines.length>0 ? lines[0].trim() : "";
                if (!first.isEmpty() && first.length() < 48) e.name = first;
                else if (!e.versionName.isEmpty()) e.name = e.versionName;
            }
            String favicon = root.optString("favicon", null);
            if (favicon != null && favicon.startsWith("data:image")) e.iconBase64 = favicon;
            e.online = true;
            if (e.pingMs < 0) e.pingMs = fallbackPing;
        } catch (Exception ex) {
            Log.w(TAG, "parse fail", ex);
            e.online = true;
            if (e.motd==null||e.motd.isEmpty()) e.motd = "A Minecraft Server";
            e.pingMs = fallbackPing;
        }
        if (e.motd==null||e.motd.isEmpty()) e.motd = "A Minecraft Server";
        if (e.pingMs < 0) e.pingMs = fallbackPing;
    }
    private static void writeVarInt(DataOutputStream out, int v) throws Exception { while ((v & 0xFFFFFF80) != 0) { out.writeByte((v & 0x7F) | 0x80); v >>>=7; } out.writeByte(v); }
    private static int readVarInt(DataInputStream in) throws Exception { int numRead=0,result=0; byte read; do{ read=in.readByte(); int val=(read & 0x7F); result|=val<<(7*numRead); numRead++; if(numRead>5) throw new RuntimeException("VarInt too big"); }while((read & 0x80)!=0); return result; }
    private static void writeString(DataOutputStream out, String s) throws Exception { byte[] b=s.getBytes(StandardCharsets.UTF_8); writeVarInt(out,b.length); out.write(b); }
}
