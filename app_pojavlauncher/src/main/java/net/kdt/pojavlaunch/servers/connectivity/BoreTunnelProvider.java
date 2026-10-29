package net.kdt.pojavlaunch.servers.connectivity;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.servers.LocalServerEntry;
import net.kdt.pojavlaunch.servers.LocalServerStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default free remote path for EVERY local server (MineServe-style).
 * On Start: auto run bore → bore.pub → public address on the server entry.
 * Friends need only Minecraft. Host does not open a separate tunnel app UI.
 *
 * Relay: third-party bore.pub (not our VPS). MIT bore client.
 */
public final class BoreTunnelProvider implements ConnectivityProvider {
    private static final String TAG = "CsBoreTunnel";
    public static final String ID = "bore_pub";

    private static final String BORE_VERSION = "0.6.0";
    private static final String URL_LINUX_AARCH64 =
            "https://github.com/ekzhang/bore/releases/download/v" + BORE_VERSION
                    + "/bore-v" + BORE_VERSION + "-aarch64-unknown-linux-musl.tar.gz";
    private static final String URL_ANDROID_AARCH64 =
            "https://raw.githubusercontent.com/Azathothas/Static-Binaries/main/bore/bore_aarch64_arm64_Android";

    private static final Pattern ADDR = Pattern.compile(
            "bore\\.pub:(\\d{2,5})", Pattern.CASE_INSENSITIVE);

    private static final ConcurrentHashMap<String, Process> AGENTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> ADDRESSES = new ConcurrentHashMap<>();

    @Override public String id() { return ID; }
    @Override public String displayName() { return "bore.pub (auto on every server)"; }

    @Override
    public String limitationsSummary() {
        return "Every server Start tries bore.pub automatically (like MineServe). "
                + "Free public relay; random port; third-party limits apply. "
                + "Friends join with only Minecraft. Local host works if tunnel fails.";
    }

    @Override
    public boolean isConfigured(Context ctx) { return true; }

    @Override
    public ConnectivityResult onServerStarted(Context ctx, LocalServerEntry server) throws Exception {
        stopAgent(server.id);
        File bin = ensureBoreBinary(ctx);
        if (bin == null) {
            return new ConnectivityResult(null, false,
                    "bore binary missing for this device. Server is local-only until fixed.");
        }
        ProcessBuilder pb = new ProcessBuilder(
                bin.getAbsolutePath(), "local", String.valueOf(server.port), "--to", "bore.pub");
        pb.redirectErrorStream(true);
        File cwd = bin.getParentFile();
        if (cwd != null) pb.directory(cwd);
        Process p = pb.start();
        AGENTS.put(server.id, p);

        String publicAddr = readAssignedAddress(p, 25);
        if (publicAddr == null) publicAddr = readAssignedAddress(p, 10);
        if (publicAddr == null) {
            stopAgent(server.id);
            return new ConnectivityResult(null, false,
                    "bore did not get a public port (relay or binary). Server still local.");
        }
        ADDRESSES.put(server.id, publicAddr);
        server.publicAddress = publicAddr;
        LocalServerStore.update(ctx, server);
        Log.i(TAG, "server " + server.id + " public " + publicAddr);
        return new ConnectivityResult(publicAddr, false,
                "Public: " + publicAddr + " (auto bore.pub). Test from another network to verify.");
    }

    @Override
    public void onServerStopped(Context ctx, LocalServerEntry server) {
        stopAgent(server.id);
        ADDRESSES.remove(server.id);
    }

    public static String lastAddress(String serverId) { return ADDRESSES.get(serverId); }

    public static void stopAgent(String serverId) {
        Process p = AGENTS.remove(serverId);
        if (p == null) return;
        try { p.destroy(); } catch (Throwable ignored) {}
        try {
            if (Build.VERSION.SDK_INT >= 26) p.destroyForcibly();
        } catch (Throwable ignored) {}
    }

    public static void stopAll() {
        for (String id : AGENTS.keySet().toArray(new String[0])) stopAgent(id);
    }

    private static String readAssignedAddress(Process p, int timeoutSec) {
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        StringBuilder buf = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            while (System.currentTimeMillis() < deadline) {
                while (br.ready()) {
                    String line = br.readLine();
                    if (line == null) break;
                    Log.i(TAG, line);
                    buf.append(line).append('\n');
                    Matcher m = ADDR.matcher(line);
                    if (m.find()) return "bore.pub:" + m.group(1);
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    try { if (!p.isAlive()) break; } catch (Throwable ignored) {}
                }
                try { Thread.sleep(250); } catch (InterruptedException ie) { break; }
            }
        } catch (Throwable t) {
            Log.w(TAG, "readAssignedAddress", t);
        }
        Matcher m2 = ADDR.matcher(buf.toString());
        if (m2.find()) return "bore.pub:" + m2.group(1);
        return null;
    }

    private static File ensureBoreBinary(Context ctx) {
        try {
            File dir = new File(Tools.DIR_GAME_NEW, "cs_tunnel/bore");
            if (!dir.exists() && !dir.mkdirs()) return null;
            File bin = new File(dir, "bore");
            if (bin.exists() && bin.canExecute() && bin.length() > 10000) return bin;

            String abi = Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                    ? Build.SUPPORTED_ABIS[0] : "unknown";
            if (!"arm64-v8a".equals(abi) && !"aarch64".equals(abi)) {
                Log.w(TAG, "unsupported ABI: " + abi);
                return null;
            }

            if (download(URL_ANDROID_AARCH64, bin)) {
                //noinspection ResultOfMethodCallIgnored
                bin.setExecutable(true, false);
                if (bin.canExecute() && bin.length() > 10000) return bin;
            }
            File tar = new File(dir, "bore.tgz");
            if (download(URL_LINUX_AARCH64, tar)) {
                try {
                    Process untar = new ProcessBuilder("tar", "-xzf", tar.getAbsolutePath(), "-C", dir.getAbsolutePath())
                            .redirectErrorStream(true).start();
                    untar.waitFor(60, TimeUnit.SECONDS);
                } catch (Throwable t) {
                    Log.w(TAG, "tar extract", t);
                }
                if (!bin.exists()) {
                    File[] kids = dir.listFiles();
                    if (kids != null) {
                        for (File f : kids) {
                            if (f.isFile() && f.length() > 10000 && !f.getName().endsWith(".tgz")
                                    && !"bore".equals(f.getName())) {
                                //noinspection ResultOfMethodCallIgnored
                                f.renameTo(bin);
                            }
                        }
                    }
                }
            }
            if (bin.exists()) {
                //noinspection ResultOfMethodCallIgnored
                bin.setExecutable(true, false);
                return bin;
            }
            return null;
        } catch (Throwable t) {
            Log.e(TAG, "ensureBoreBinary", t);
            return null;
        }
    }

    private static boolean download(String urlStr, File dest) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setConnectTimeout(20000);
            c.setReadTimeout(120000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "CS-Launcher/3");
            try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            return dest.length() > 1000;
        } catch (Throwable t) {
            Log.e(TAG, "download " + urlStr, t);
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
