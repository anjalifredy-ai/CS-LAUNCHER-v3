package net.kdt.pojavlaunch.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bridges process console lines to UI (Activity / Fragment).
 * Implements {@link CsServerProcessManager.ConsoleListener}.
 */
public class CsServerConsoleBridge implements CsServerProcessManager.ConsoleListener,
        CsServerProcessManager.StatusListener {

    public interface UiSink {
        void onConsoleLine(String serverId, String line);
        void onStatusChanged(String serverId, CsServerRuntimeStatus status);
    }

    private final List<UiSink> sinks = new CopyOnWriteArrayList<>();
    private final List<String> ringBuffer = new ArrayList<>();
    private static final int MAX_BUFFER = 500;
    private final Object lock = new Object();

    public void addSink(UiSink sink) {
        sinks.add(sink);
        synchronized (lock) {
            for (String line : ringBuffer) {
                int sep = line.indexOf('|');
                if (sep > 0) {
                    sink.onConsoleLine(line.substring(0, sep), line.substring(sep + 1));
                }
            }
        }
    }

    public void removeSink(UiSink sink) {
        sinks.remove(sink);
    }

    /** Attach to a process manager once. */
    public void attach(CsServerProcessManager pm) {
        pm.addConsoleListener(this);
        pm.addStatusListener(this);
    }

    public void detach(CsServerProcessManager pm) {
        pm.removeConsoleListener(this);
        pm.removeStatusListener(this);
    }

    @Override
    public void onConsoleLine(CsServerProcessManager.ConsoleLine line) {
        synchronized (lock) {
            ringBuffer.add(line.serverId + "|" + line.line);
            while (ringBuffer.size() > MAX_BUFFER) {
                ringBuffer.remove(0);
            }
        }
        for (UiSink s : sinks) {
            try {
                s.onConsoleLine(line.serverId, line.line);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onStatusChanged(String serverId, CsServerRuntimeStatus status) {
        for (UiSink s : sinks) {
            try {
                s.onStatusChanged(serverId, status);
            } catch (Exception ignored) {}
        }
    }

    public List<String> snapshotLines() {
        synchronized (lock) {
            return new ArrayList<>(ringBuffer);
        }
    }
}
