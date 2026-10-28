package net.kdt.pojavlaunch.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bridges process console lines to UI (Activity / Fragment).
 * Thread-safe listener list.
 */
public class CsServerConsoleBridge implements CsServerProcessManager.ConsoleListener {

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
                // replay without server id prefix format "id|line"
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

    @Override
    public void onLine(String serverId, String line) {
        synchronized (lock) {
            ringBuffer.add(serverId + "|" + line);
            while (ringBuffer.size() > MAX_BUFFER) {
                ringBuffer.remove(0);
            }
        }
        for (UiSink s : sinks) {
            try {
                s.onConsoleLine(serverId, line);
            } catch (Exception ignored) {}
        }
    }

    public void notifyStatus(String serverId, CsServerRuntimeStatus status) {
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
