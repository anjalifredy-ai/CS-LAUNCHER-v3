package net.kdt.pojavlaunch.server;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs at most one active Minecraft Java server process on-device.
 * Real ProcessBuilder. Real stdout. ONLINE only after readiness markers.
 */
public class CsServerProcessManager {

    private static final String TAG = "CsServerProcess";

    public static class ConsoleLine {
        public final String serverId;
        public final String line;
        public final long timestamp;

        public ConsoleLine(String serverId, String line) {
            this.serverId = serverId;
            this.line = line;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public interface ConsoleListener {
        void onConsoleLine(ConsoleLine line);
    }

    public interface StatusListener {
        void onStatusChanged(String serverId, CsServerRuntimeStatus status);
    }

    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    private final Map<String, BufferedWriter> writers = new ConcurrentHashMap<>();
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();
    private final Map<String, CsServerRuntimeStatus> statusMap = new ConcurrentHashMap<>();
    private final List<ConsoleListener> consoleListeners = new CopyOnWriteArrayList<>();
    private final List<StatusListener> statusListeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final CsServerRamAdvisor ramAdvisor;

    public CsServerProcessManager(CsServerRamAdvisor ramAdvisor) {
        this.ramAdvisor = ramAdvisor;
    }

    public void addConsoleListener(ConsoleListener l) {
        consoleListeners.add(l);
    }

    public void removeConsoleListener(ConsoleListener l) {
        consoleListeners.remove(l);
    }

    public void addStatusListener(StatusListener l) {
        statusListeners.add(l);
    }

    public void removeStatusListener(StatusListener l) {
        statusListeners.remove(l);
    }

    /**
     * @param javaBin absolute path to java executable (from installed JRE)
     */
    public synchronized String start(CsServerConfig config, String javaBin) {
        if (processes.containsKey(config.id)) {
            return "Server already running";
        }
        if (!processes.isEmpty()) {
            return "Another server is already running. Stop it first.";
        }
        if (!config.eulaAccepted) {
            return "Minecraft EULA not accepted.";
        }
        if (ramAdvisor != null && !ramAdvisor.isAllocationSafe(config.memoryMb)) {
            return "Requested " + config.memoryMb + " MB outside safe range.";
        }

        File serverDir = new File(config.serverDir);
        File jar = new File(serverDir, "server.jar");
        if (!jar.exists()) {
            return "server.jar not found. Install first.";
        }
        if (javaBin == null || javaBin.isEmpty() || !new File(javaBin).exists()) {
            return "Java binary not found: " + javaBin;
        }

        updateStatus(config.id, CsServerState.STARTING, config.memoryMb, null);

        try {
            List<String> args = new ArrayList<>();
            args.add(javaBin);
            int xms = Math.max(config.memoryMb / 2, 256);
            args.add("-Xms" + xms + "m");
            args.add("-Xmx" + config.memoryMb + "m");
            args.add("-XX:+UseG1GC");
            args.add("-XX:+ParallelRefProcEnabled");
            args.add("-Djava.awt.headless=true");
            if (config.jvmArgs != null && !config.jvmArgs.trim().isEmpty()) {
                for (String part : config.jvmArgs.trim().split("\\s+")) {
                    if (!part.isEmpty()) args.add(part);
                }
            }
            args.add("-jar");
            args.add(jar.getAbsolutePath());
            args.add("nogui");

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(serverDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            processes.put(config.id, process);
            startTimes.put(config.id, System.currentTimeMillis());
            writers.put(config.id, new BufferedWriter(new OutputStreamWriter(process.getOutputStream())));

            final String serverId = config.id;
            final int mem = config.memoryMb;
            executor.execute(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        ConsoleLine cl = new ConsoleLine(serverId, line);
                        for (ConsoleListener l : consoleListeners) {
                            try { l.onConsoleLine(cl); } catch (Exception ignored) {}
                        }
                        if (isReadyMarker(line)) {
                            updateStatus(serverId, CsServerState.ONLINE, mem, null);
                        }
                        if (line.toLowerCase().contains("failed to start") ||
                                line.contains("OutOfMemoryError")) {
                            updateStatus(serverId, CsServerState.FAILED, mem, line);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Server output read error", e);
                } finally {
                    int code = -1;
                    try {
                        code = process.waitFor();
                    } catch (Exception ignored) {}
                    cleanup(serverId);
                    updateStatus(serverId,
                            code == 0 ? CsServerState.STOPPED : CsServerState.FAILED,
                            mem,
                            code != 0 ? "Exit code " + code : null);
                }
            });

            return null; // success
        } catch (Exception e) {
            cleanup(config.id);
            updateStatus(config.id, CsServerState.FAILED, config.memoryMb, e.getMessage());
            return e.getMessage() != null ? e.getMessage() : "Start failed";
        }
    }

    public synchronized void stop(String serverId, boolean force) {
        Process process = processes.get(serverId);
        if (process == null) return;

        CsServerRuntimeStatus cur = statusMap.get(serverId);
        int mem = cur != null ? (int) cur.memoryMaxMb : 0;
        updateStatus(serverId, CsServerState.STOPPING, mem, null);

        try {
            if (!force) {
                sendCommand(serverId, "stop");
                long waited = 0;
                while (ProcessCompat.isAlive(process) && waited < 15000) {
                    Thread.sleep(500);
                    waited += 500;
                }
            }
            if (ProcessCompat.isAlive(process)) {
                ProcessCompat.destroyForcibly(process);
            }
        } catch (Exception e) {
            Log.e(TAG, "Stop failed", e);
            try { ProcessCompat.destroyForcibly(process); } catch (Exception ignored) {}
        } finally {
            cleanup(serverId);
            updateStatus(serverId, CsServerState.STOPPED, 0, null);
        }
    }

    public boolean sendCommand(String serverId, String command) {
        BufferedWriter writer = writers.get(serverId);
        if (writer == null) return false;
        try {
            writer.write(command);
            writer.newLine();
            writer.flush();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Command send failed", e);
            return false;
        }
    }

    public CsServerRuntimeStatus getStatus(String serverId) {
        CsServerRuntimeStatus s = statusMap.get(serverId);
        return s != null ? s : new CsServerRuntimeStatus(serverId, CsServerState.STOPPED);
    }

    public boolean isAnyRunning() {
        return !processes.isEmpty();
    }

    private boolean isReadyMarker(String line) {
        String lower = line.toLowerCase();
        return lower.contains("done (") ||
                lower.contains("for help, type") ||
                (lower.contains("started") && lower.contains("server"));
    }

    private void cleanup(String serverId) {
        try {
            BufferedWriter w = writers.remove(serverId);
            if (w != null) w.close();
        } catch (Exception ignored) {}
        processes.remove(serverId);
        startTimes.remove(serverId);
    }

    private void updateStatus(String serverId, CsServerState state, int memoryMaxMb, String error) {
        long uptime = 0;
        Long start = startTimes.get(serverId);
        if (start != null) uptime = System.currentTimeMillis() - start;

        CsServerRuntimeStatus status = new CsServerRuntimeStatus();
        status.serverId = serverId;
        status.state = state;
        status.uptimeMs = uptime;
        status.memoryMaxMb = memoryMaxMb;
        status.lastError = error;
        Process p = processes.get(serverId);
        if (p != null) {
            status.pid = ProcessCompat.pid(p);
        }
        statusMap.put(serverId, status);

        for (StatusListener l : statusListeners) {
            try { l.onStatusChanged(serverId, status); } catch (Exception ignored) {}
        }
    }
}
