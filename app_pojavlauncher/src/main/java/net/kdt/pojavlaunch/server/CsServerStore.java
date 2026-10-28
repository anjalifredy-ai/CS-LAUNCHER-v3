package net.kdt.pojavlaunch.server;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Persists server configs as JSON under CS servers/index.json.
 */
public class CsServerStore {

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<CsServerConfig>>(){}.getType();

    private final Context context;
    private final List<CsServerConfig> servers = new CopyOnWriteArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();

    public interface Listener {
        void onServersChanged(List<CsServerConfig> servers);
    }

    public CsServerStore(Context context) {
        this.context = context.getApplicationContext();
    }

    private File serversRoot() {
        File root = new File(Tools.DIR_GAME_HOME, "../servers");
        if (!root.exists()) root.mkdirs();
        return root;
    }

    private File indexFile() {
        return new File(serversRoot(), "index.json");
    }

    public synchronized void load() {
        File idx = indexFile();
        if (!idx.exists()) {
            servers.clear();
            notifyListeners();
            return;
        }
        try (FileReader reader = new FileReader(idx)) {
            List<CsServerConfig> list = GSON.fromJson(reader, LIST_TYPE);
            servers.clear();
            if (list != null) servers.addAll(list);
        } catch (Exception e) {
            servers.clear();
        }
        notifyListeners();
    }

    public synchronized void save(CsServerConfig config) {
        config.updatedAt = System.currentTimeMillis();
        boolean found = false;
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).id.equals(config.id)) {
                servers.set(i, config);
                found = true;
                break;
            }
        }
        if (!found) servers.add(config);
        persist();
        notifyListeners();
    }

    public synchronized void delete(String serverId) {
        servers.removeIf(s -> s.id.equals(serverId));
        persist();
        notifyListeners();
    }

    public CsServerConfig getById(String serverId) {
        for (CsServerConfig s : servers) {
            if (s.id.equals(serverId)) return s;
        }
        return null;
    }

    public List<CsServerConfig> getAll() {
        return new ArrayList<>(servers);
    }

    public String createId() {
        return UUID.randomUUID().toString();
    }

    public File defaultServerDir(String serverId) {
        File dir = new File(serversRoot(), serverId);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void persist() {
        try {
            File idx = indexFile();
            idx.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(idx)) {
                GSON.toJson(servers, w);
            }
        } catch (Exception ignored) {}
    }

    private void notifyListeners() {
        List<CsServerConfig> snapshot = getAll();
        for (Listener l : listeners) {
            try {
                l.onServersChanged(snapshot);
            } catch (Exception ignored) {}
        }
    }
}
