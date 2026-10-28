package net.kdt.pojavlaunch.server;

import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * API-21 safe Process helpers. isAlive()/destroyForcibly()/pid() are API 26+.
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
            sDestroyForcibly = Process.class.getMethod("destroyForcibly");
            try {
                sPid = Process.class.getMethod("pid");
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable t) {
            Log.w(TAG, "reflect Process methods", t);
        }
    }

    public static boolean isAlive(Process p) {
        if (p == null) return false;
        if (Build.VERSION.SDK_INT >= 26) {
            return p.isAlive();
        }
        try {
            p.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    public static void destroyForcibly(Process p) {
        if (p == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            p.destroyForcibly();
            return;
        }
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

    public static long pid(Process p) {
        if (p == null) return -1;
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                return p.pid();
            } catch (Throwable ignored) {}
        }
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
