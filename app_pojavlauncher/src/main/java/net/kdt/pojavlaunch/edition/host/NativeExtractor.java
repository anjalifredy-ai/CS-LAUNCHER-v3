package net.kdt.pojavlaunch.edition.host;

import android.content.Context;
import android.os.Build;

import net.kdt.pojavlaunch.edition.BedrockVersionManager;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extract lib/&lt;abi&gt;/*.so from imported APK into isolation natives/.
 * Does NOT System.load / loadLibrary.
 */
public final class NativeExtractor {
    public static final String MARKER = "extract_meta.json";

    private NativeExtractor() {}

    public static final class ExtractResult {
        public final boolean ok;
        public final String abi;
        public final int count;
        public final File outDir;
        public final String error;

        public ExtractResult(boolean ok, String abi, int count, File outDir, String error) {
            this.ok = ok;
            this.abi = abi;
            this.count = count;
            this.outDir = outDir;
            this.error = error;
        }

        public String summary() {
            if (!ok) return "extract fail: " + error;
            return "extracted " + count + " .so → " + abi;
        }
    }

    /** Device ABI preference order. */
    public static String[] deviceAbis() {
        if (Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS != null) {
            return Build.SUPPORTED_ABIS;
        }
        return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
    }

    public static String pickAbi(ApkNativeInventory.Result inv) {
        if (inv == null || !inv.ok || inv.byAbi.isEmpty()) return null;
        for (String deviceAbi : deviceAbis()) {
            if (deviceAbi == null) continue;
            if (inv.byAbi.containsKey(deviceAbi)) return deviceAbi;
        }
        // fallback first available
        return inv.byAbi.keySet().iterator().next();
    }

    public static File nativesDir(Context c, String versionId, String abi) {
        File f = new File(new File(BedrockVersionManager.isolationDir(c, versionId), "natives"), abi);
        //noinspection ResultOfMethodCallIgnored
        f.mkdirs();
        return f;
    }

    public static boolean hasExtracted(Context c, String versionId) {
        File nativesRoot = new File(BedrockVersionManager.isolationDir(c, versionId), "natives");
        if (!nativesRoot.isDirectory()) return false;
        File[] abis = nativesRoot.listFiles();
        if (abis == null) return false;
        for (File abiDir : abis) {
            if (!abiDir.isDirectory()) continue;
            File[] sos = abiDir.listFiles((dir, name) -> name != null && name.endsWith(".so"));
            if (sos != null && sos.length > 0) return true;
        }
        return false;
    }

    public static int countExtracted(Context c, String versionId) {
        int n = 0;
        File nativesRoot = new File(BedrockVersionManager.isolationDir(c, versionId), "natives");
        File[] abis = nativesRoot.listFiles();
        if (abis == null) return 0;
        for (File abiDir : abis) {
            if (!abiDir.isDirectory()) continue;
            File[] sos = abiDir.listFiles((dir, name) -> name != null && name.endsWith(".so"));
            if (sos != null) n += sos.length;
        }
        return n;
    }

    public static ExtractResult extractForDevice(Context c, String versionId) {
        if (c == null || versionId == null) {
            return new ExtractResult(false, null, 0, null, "null args");
        }
        File apk = BedrockVersionManager.apkFile(c, versionId);
        if (!apk.isFile()) {
            return new ExtractResult(false, null, 0, null, "APK missing");
        }
        ApkNativeInventory.Result inv = ApkNativeInventory.scanAndSave(c, versionId);
        if (!inv.ok) {
            return new ExtractResult(false, null, 0, null, inv.error);
        }
        String abi = pickAbi(inv);
        if (abi == null) {
            return new ExtractResult(false, null, 0, null, "no ABI in APK");
        }
        List<String> libs = inv.byAbi.get(abi);
        if (libs == null || libs.isEmpty()) {
            return new ExtractResult(false, abi, 0, null, "no libs for " + abi);
        }

        File outDir = nativesDir(c, versionId, abi);
        // clear old .so in this abi folder
        File[] old = outDir.listFiles();
        if (old != null) {
            for (File f : old) {
                if (f.isFile() && f.getName().endsWith(".so")) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        }

        int count = 0;
        try (ZipFile zip = new ZipFile(apk)) {
            for (String soName : libs) {
                String entryName = "lib/" + abi + "/" + soName;
                ZipEntry entry = zip.getEntry(entryName);
                if (entry == null) continue;
                File dest = new File(outDir, soName);
                try (InputStream in = zip.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
                }
                count++;
            }
        } catch (Exception e) {
            return new ExtractResult(false, abi, count, outDir, e.getMessage());
        }

        try {
            JSONObject meta = new JSONObject();
            meta.put("abi", abi);
            meta.put("count", count);
            meta.put("libs", new org.json.JSONArray(libs));
            File marker = new File(BedrockVersionManager.isolationDir(c, versionId), "meta/" + MARKER);
            try (FileOutputStream fos = new FileOutputStream(marker)) {
                fos.write(meta.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}

        return new ExtractResult(count > 0, abi, count, outDir,
                count > 0 ? null : "zero files extracted");
    }
}
