package net.kdt.pojavlaunch.edition.host;

import android.content.Context;

import net.kdt.pojavlaunch.edition.BedrockIsolationPaths;
import net.kdt.pojavlaunch.edition.BedrockVersionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Scan imported official APK for native libs (Zip read only).
 * Does NOT extract or System.loadLibrary.
 */
public final class ApkNativeInventory {
    public static final String INVENTORY_FILE = "native_inventory.json";

    private ApkNativeInventory() {}

    public static final class Result {
        public final boolean ok;
        public final int totalSo;
        public final Map<String, List<String>> byAbi;
        public final String error;

        public Result(boolean ok, int totalSo, Map<String, List<String>> byAbi, String error) {
            this.ok = ok;
            this.totalSo = totalSo;
            this.byAbi = byAbi != null ? byAbi : Collections.emptyMap();
            this.error = error;
        }

        public String shortSummary() {
            if (!ok) return "inventory fail: " + error;
            StringBuilder sb = new StringBuilder("so=" + totalSo);
            for (Map.Entry<String, List<String>> e : byAbi.entrySet()) {
                sb.append(" ").append(e.getKey()).append("=").append(e.getValue().size());
            }
            return sb.toString();
        }
    }

    public static Result scanApkFile(File apk) {
        if (apk == null || !apk.isFile()) {
            return new Result(false, 0, null, "APK missing");
        }
        Map<String, List<String>> byAbi = new LinkedHashMap<>();
        int total = 0;
        try (ZipFile zip = new ZipFile(apk)) {
            for (ZipEntry entry : Collections.list(zip.entries())) {
                String name = entry.getName();
                if (name == null || entry.isDirectory()) continue;
                // lib/<abi>/<name.so>
                if (!name.startsWith("lib/") || !name.endsWith(".so")) continue;
                String rest = name.substring(4); // after lib/
                int slash = rest.indexOf('/');
                if (slash <= 0) continue;
                String abi = rest.substring(0, slash);
                String so = rest.substring(slash + 1);
                if (so.contains("/")) continue; // nested skip
                List<String> list = byAbi.get(abi);
                if (list == null) {
                    list = new ArrayList<>();
                    byAbi.put(abi, list);
                }
                list.add(so);
                total++;
            }
            for (List<String> list : byAbi.values()) {
                Collections.sort(list);
            }
            return new Result(true, total, byAbi, null);
        } catch (Exception e) {
            return new Result(false, 0, null, e.getMessage());
        }
    }

    public static Result scanAndSave(Context c, String versionId) {
        File apk = BedrockVersionManager.apkFile(c, versionId);
        Result r = scanApkFile(apk);
        if (!r.ok) return r;
        try {
            JSONObject root = new JSONObject();
            root.put("versionId", versionId);
            root.put("apkPath", apk.getAbsolutePath());
            root.put("totalSo", r.totalSo);
            JSONObject abis = new JSONObject();
            for (Map.Entry<String, List<String>> e : r.byAbi.entrySet()) {
                JSONArray arr = new JSONArray();
                for (String s : e.getValue()) arr.put(s);
                abis.put(e.getKey(), arr);
            }
            root.put("abis", abis);
            File out = new File(BedrockIsolationPaths.metaDir(c, versionId), INVENTORY_FILE);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            return new Result(false, r.totalSo, r.byAbi, "save failed: " + e.getMessage());
        }
        return r;
    }

    public static String loadSummary(Context c, String versionId) {
        File f = new File(BedrockIsolationPaths.metaDir(c, versionId), INVENTORY_FILE);
        if (!f.isFile()) {
            if (BedrockVersionManager.hasFullApk(c, versionId)) {
                Result r = scanAndSave(c, versionId);
                return r.shortSummary();
            }
            return "no inventory";
        }
        try {
            byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
            JSONObject o = new JSONObject(new String(data, StandardCharsets.UTF_8));
            int total = o.optInt("totalSo", 0);
            JSONObject abis = o.optJSONObject("abis");
            StringBuilder sb = new StringBuilder("so=" + total);
            if (abis != null) {
                JSONArray names = abis.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String abi = names.getString(i);
                        sb.append(" ").append(abi).append("=")
                                .append(abis.getJSONArray(abi).length());
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            // API 26+ Files; fallback scan
            Result r = scanApkFile(BedrockVersionManager.apkFile(c, versionId));
            return r.shortSummary();
        }
    }
}
