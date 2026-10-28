package net.kdt.pojavlaunch.server;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Spawns a dedicated server process (java -jar server.jar) with console capture.
 * Uses host JRE path provided by caller (CS / Pojav runtime).
 */
public class CsServerProcessManager {

    private static final String TAG = "CsServerProcess";

    public enum State { STOPPED, STARTING, ONLINE, STOPPING, FAILED }

    public static class RuntimeStatus {
        public State state = State.STOPPED;
        public long startedAt;
        public String lastError;
    }

    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    private final Map<String, RuntimeStatus> statuses = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<String>>> logListeners = new ConcurrentHashMap<>();

    public RuntimeStatus getStatus(String serverId) {
        return statuses.computeIfAbsent(serverId, k -> new RuntimeStatus());
    }

    public void addLogListener(String serverId, Consumer<String> listener) {
        logListeners.computeIfAbsent(serverId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public synchronized void start(CsServerConfig config, String javaBinary) throws Exception {
        if (processes.containsKey(config.id)) {
            throw new IllegalStateException("Already running");
        }
        File dir = new File(config.serverDir);
        if (!dir.exists()) dir.mkdirs();

        File jar = findServerJar(dir);
        if (jar == null) {
            throw new IllegalStateException("No server jar in " + dir.getAbsolutePath() +
                    " — install Paper/Vanilla/Fabric jar first");
        }

        writeEula(dir);
        writeProperties(config, dir);

        RuntimeStatus st = getStatus(config.id);
        st.state = State.STARTING;
        st.lastError = null;
        st.startedAt = System.currentTimeMillis();

        List<String> cmd = new ArrayList<>();
        cmd.add(javaBinary);
        cmd.add("-Xms" + Math.max(256, config.memoryMb / 2) + "m");
        cmd.add("-Xmx" + config.memoryMb + "m");
        cmd.add("-XX:+UseG1GC");
        cmd.add("-jar");
        cmd.add(jar.getAbsolutePath());
        cmd.add("nogui");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        processes.put(config.id, p);

        Thread t = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    emitLog(config.id, line);
                    if (line.contains("Done") && line.contains("For help")) {
                        getStatus(config.id).state = State.ONLINE;
                    }
                }
                int code = p.waitFor();
                processes.remove(config.id);
                RuntimeStatus s = getStatus(config.id);
                s.state = code == 0 ? State.STOPPED : State.FAILED;
                if (code != 0) s.lastError = "Exit code " + code;
            } catch (Exception e) {
                Log.e(TAG, "server log", e);
                getStatus(config.id).state = State.FAILED;
                getStatus(config.id).lastError = e.getMessage();
                processes.remove(config.id);
            }
        }, "cs-server-" + config.id);
        t.setDaemon(true);
        t.start();
    }

    public synchronized void stop(String serverId) {
        Process p = processes.get(serverId);
        if (p == null) return;
        getStatus(serverId).state = State.STOPPING;
        try {
            OutputStreamWriter w = new OutputStreamWriter(p.getOutputStream());
            w.write("stop\n");
            w.flush();
        } catch (Exception ignored) {}
        new Thread(() -> {
            try {
                if (!p.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }
            } catch (Exception e) {
                p.destroyForcibly();
            }
            processes.remove(serverId);
            getStatus(serverId).state = State.STOPPED;
        }).start();
    }

    public void sendCommand(String serverId, String command) {
        Process p = processes.get(serverId);
        if (p == null) return;
        try {
            OutputStreamWriter w = new OutputStreamWriter(p.getOutputStream());
            w.write(command.endsWith("\n") ? command : command + "\n");
            w.flush();
        } catch (Exception e) {
            Log.e(TAG, "cmd", e);
        }
    }

    private void emitLog(String id, String line) {
        List<Consumer<String>> ls = logListeners.get(id);
        if (ls != null) for (Consumer<String> c : ls) {
            try { c.accept(line); } catch (Exception ignored) {}
        }
    }

    private static File findServerJar(File dir) {
        File[] files = dir.listFiles((d, n) -> n.endsWith(".jar") && !n.contains("installer"));
        if (files == null || files.length == 0) return null;
        // prefer server.jar / paper*.jar
        for (File f : files) {
            String n = f.getName().toLowerCase();
            if (n.equals("server.jar") || n.startsWith("paper") || n.startsWith("purpur")
                    || n.contains("fabric") || n.contains("forge")) return f;
        }
        return files[0];
    }

    private static void writeEula(File dir) throws Exception {
        File eula = new File(dir, "eula.txt");
        if (!eula.exists()) {
            try (FileOutputStream fos = new FileOutputStream(eula)) {
                fos.write("eula=true\n".getBytes());
            }
        }
    }

    private static void writeProperties(CsServerConfig c, File dir) throws Exception {
        File props = new File(dir, "server.properties");
        StringBuilder sb = new StringBuilder();
        sb.append("server-port=").append(c.port).append('\n');
        sb.append("max-players=").append(c.maxPlayers).append('\n');
        sb.append("online-mode=").append(c.onlineMode).append('\n');
        sb.append("white-list=").append(c.whitelist).append('\n');
        sb.append("pvp=").append(c.pvp).append('\n');
        sb.append("motd=").append(c.motd).append('\n');
        sb.append("level-name=").append(c.worldDir).append('\n');
        try (FileOutputStream fos = new FileOutputStream(props)) {
            fos.write(sb.toString().getBytes());
        }
    }

    // local import for FileOutputStream without full import clutter in snippet above
    private static class FileOutputStream extends java.io.FileOutputStream {
        FileOutputStream(File f) throws java.io.FileNotFoundException { super(f); }
    }
}
