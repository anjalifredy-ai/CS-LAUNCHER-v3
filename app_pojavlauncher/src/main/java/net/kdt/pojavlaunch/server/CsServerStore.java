package net.kdt.pojavlaunch.server;

import net.kdt.pojavlaunch.Tools;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Persist server list under game directory. */
public class CsServerStore {

    private final File storeFile;

    public CsServerStore() {
        File dir = new File(Tools.DIR_GAME_HOME, "cs_servers");
        if (!dir.exists()) dir.mkdirs();
        storeFile = new File(dir, "servers.json");
    }

    public synchronized List<CsServerConfig> loadAll() {
        List<CsServerConfig> list = new ArrayList<>();
        if (!storeFile.exists()) return list;
        try {
            byte[] data = readAll(storeFile);
            JSONArray arr = new JSONArray(new String(data, StandardCharsets.UTF_8));
            for (int i = 0; i < arr.length(); i++) {
                list.add(CsServerConfig.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized void saveAll(List<CsServerConfig> list) {
        try {
            JSONArray arr = new JSONArray();
            for (CsServerConfig c : list) arr.put(c.toJson());
            writeAll(storeFile, arr.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void upsert(CsServerConfig config) {
        List<CsServerConfig> list = loadAll();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(config.id)) {
                list.set(i, config);
                found = true;
                break;
            }
        }
        if (!found) list.add(config);
        saveAll(list);
    }

    public synchronized void delete(String id) {
        List<CsServerConfig> list = loadAll();
        list.removeIf(c -> c.id.equals(id));
        saveAll(list);
    }

    public CsServerConfig getById(String id) {
        for (CsServerConfig c : loadAll()) {
            if (c.id.equals(id)) return c;
        }
        return null;
    }

    private static byte[] readAll(File f) throws Exception {
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int off = 0;
            while (off < buf.length) {
                int n = in.read(buf, off, buf.length - off);
                if (n < 0) break;
                off += n;
            }
            return buf;
        }
    }

    private static void writeAll(File f, byte[] data) throws Exception {
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }
    }
}
