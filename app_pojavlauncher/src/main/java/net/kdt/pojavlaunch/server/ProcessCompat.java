package net.kdt.pojavlaunch.server;

import android.util.Log;

import java.lang.reflect.Method;

/**
 * API-21 safe Process helpers.
 * Never call Process.isAlive / destroyForcibly / pid directly — those methods
 * are API 26+ and may be missing from the compile classpath / cause hard errors.
 * All access is via reflection or exitValue fallback.
 */
public final class ProcessCompat {
    private static final String TAG = "ProcessCompat";
    private static Method sIsAlive;
    private static Method sDestroyForcibly;
    private static Method sPid;
    private static boolean sReflectTried;

    private ProcessCompat() {}

    private static void ensureReflect() {
        if (sReflectTried) return;
        sReflectTried = true;
        try {
            sIsAlive = Process.class.getMethod("isAlive");
        } catch (Throwable t) {
            Log.w(TAG, "no isAlive", t);
        }
        try {
            sDestroyForcibly = Process.class.getMethod("destroyForcibly");
        } catch (Throwable t) {
            Log.w(TAG, "no destroyForcibly", t);
        }
        try {
            sPid = Process.class.getMethod("pid");
        } catch (Throwable t) {
            Log.w(TAG, "no pid", t);
        }
    }

    public static boolean isAlive(Process p) {
        if (p == null) return false;
        ensureReflect();
        if (sIsAlive != null) {
            try {
                Object v = sIsAlive.invoke(p);
                if (v instanceof Boolean) return (Boolean) v;
            } catch (Throwable ignored) {}
        }
        try {
            p.exitValue();
            return false; // already exited
        } catch (IllegalThreadStateException e) {
            return true; // still running
        } catch (Throwable t) {
            return false;
        }
    }

    public static void destroyForcibly(Process p) {
        if (p == null) return;
        ensureReflect();
        if (sDestroyForcibly != null) {
            try {
                sDestroyForcibly.invoke(p);
                return;
            } catch (Throwable ignored) {}
        }
        try {
            p.destroy();
        } catch (Throwable ignored) {}
    }

    /** @return process pid, or -1 if unavailable */
    public static long pid(Process p) {
        if (p == null) return -1;
        ensureReflect();
        if (sPid != null) {
            try {
                Object v = sPid.invoke(p);
                if (v instanceof Integer) return ((Integer) v).longValue();
                if (v instanceof Long) return (Long) v;
            } catch (Throwable ignored) {}
        }
        return -1;
    }
}
