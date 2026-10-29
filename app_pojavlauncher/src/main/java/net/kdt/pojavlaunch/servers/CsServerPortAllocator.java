package net.kdt.pojavlaunch.servers;

import android.content.Context;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Picks a free local TCP port starting at 25565 (auto-generate on create). */
public final class CsServerPortAllocator {
    private CsServerPortAllocator() {}

    public static int allocate(Context ctx) {
        Set<Integer> used = new HashSet<>();
        try {
            List<LocalServerEntry> list = LocalServerStore.list(ctx);
            if (list != null) {
                for (LocalServerEntry e : list) {
                    if (e != null && e.port > 0) used.add(e.port);
                }
            }
        } catch (Throwable ignored) {}
        for (int p = 25565; p <= 25665; p++) {
            if (used.contains(p)) continue;
            if (isFree(p)) return p;
        }
        for (int p = 25565; p < 26000; p++) {
            if (isFree(p)) return p;
        }
        return 25565;
    }

    private static boolean isFree(int port) {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (ss != null) try { ss.close(); } catch (IOException ignored) {}
        }
    }
}
