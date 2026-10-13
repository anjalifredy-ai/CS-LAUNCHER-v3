package net.kdt.pojavlaunch.edition.host;

import android.content.Context;
import android.os.Build;

import net.kdt.pojavlaunch.edition.BedrockVersionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Safe experiment: System.load on extracted .so paths from owned APK.
 * Records results only — does not start the game.
 */
public final class NativeLoadProbe {
    public static final String PROBE_FILE = "load_probe.json";

    private NativeLoadProbe() {}

    public static final class ProbeResult {
        public final int attempted;
        public final int loaded;
        public final List<String> okNames;
        public final List<String> failLines;
        public final String note;

        public ProbeResult(int attempted, int loaded, List<String> okNames,
                           List<String> failLines, String note) {
            this.attempted = attempted;
            this.loaded = loaded;
            this.okNames = okNames;
            this.failLines = failLines;
            this.note = note;
        }

        public String summary() {
            return "probe load " + loaded + "/" + attempted + " — NOT a game start. " + note;
        }
    }

    public static boolean hasProbeLog(Context c, String versionId) {
        return probeFile(c, versionId).isFile();
    }

    private static File probeFile(Context c, String versionId) {
        return new File(new File(BedrockVersionManager.isolationDir(c, versionId), "meta"), PROBE_FILE);
    }

    public static String lastSummary(Context c, String versionId) {
        File f = probeFile(c, versionId);
        if (!f.isFile()) return "no probe yet";
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) >= 0) bos.write(buf, 0, n);
            }
            JSONObject o = new JSONObject(new String(bos.toByteArray(), StandardCharsets.UTF_8));
            return "last probe " + o.optInt("loaded", 0) + "/" + o.optInt("attempted", 0)
                    + " (game still not started)";
        } catch (Exception e) {
            return "probe log unreadable";
        }
    }

    /**
     * Load up to maxLibs extracted .so files. Failures are caught and logged.
     */
    public static ProbeResult run(Context c, String versionId, int maxLibs) {
        List<String> ok = new ArrayList<>();
        List<String> fail = new ArrayList<>();
        if (c == null || versionId == null) {
            return new ProbeResult(0, 0, ok, fail, "null args");
        }
        if (!NativeExtractor.hasExtracted(c, versionId)) {
            NativeExtractor.extractForDevice(c, versionId);
        }
        File nativesRoot = new File(BedrockVersionManager.isolationDir(c, versionId), "natives");
        List<File> soFiles = new ArrayList<>();
        File[] abiDirs = nativesRoot.listFiles();
        if (abiDirs != null) {
            for (File abiDir : abiDirs) {
                if (!abiDir.isDirectory()) continue;
                File[] sos = abiDir.listFiles((dir, name) -> name != null && name.endsWith(".so"));
                if (sos == null) continue;
                for (File so : sos) soFiles.add(so);
            }
        }
        if (soFiles.isEmpty()) {
            return save(c, versionId, new ProbeResult(0, 0, ok, fail, "no extracted .so"));
        }

        // Prefer smaller libs first — large game engines often need deps; still experimental
        soFiles.sort((a, b) -> Long.compare(a.length(), b.length()));
        int limit = Math.max(1, Math.min(maxLibs, soFiles.size()));
        int attempted = 0;
        for (int i = 0; i < limit; i++) {
            File so = soFiles.get(i);
            attempted++;
            try {
                System.load(so.getAbsolutePath());
                ok.add(so.getName());
            } catch (UnsatisfiedLinkError e) {
                fail.add(so.getName() + ": " + e.getMessage());
            } catch (Throwable t) {
                fail.add(so.getName() + ": " + t.getClass().getSimpleName() + " " + t.getMessage());
            }
        }
        String note = "ABI device=" + (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                ? Build.SUPPORTED_ABIS[0] : "?")
                + ". Partial load ≠ Minecraft running.";
        return save(c, versionId, new ProbeResult(attempted, ok.size(), ok, fail, note));
    }

    private static ProbeResult save(Context c, String versionId, ProbeResult r) {
        try {
            JSONObject o = new JSONObject();
            o.put("attempted", r.attempted);
            o.put("loaded", r.loaded);
            o.put("ok", new JSONArray(r.okNames));
            o.put("fail", new JSONArray(r.failLines));
            o.put("note", r.note);
            o.put("gameStarted", false);
            File out = probeFile(c, versionId);
            //noinspection ResultOfMethodCallIgnored
            out.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(o.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
        return r;
    }
}
