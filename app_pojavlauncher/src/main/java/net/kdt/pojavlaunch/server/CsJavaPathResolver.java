package net.kdt.pojavlaunch.server;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.Runtime;

import java.io.File;

/**
 * Resolves absolute path to {@code bin/java} for a required major version
 * using installed MultiRT runtimes (Java 8 / 17 / 21).
 */
public final class CsJavaPathResolver {

    private CsJavaPathResolver() {}

    public static String resolveJavaBin(int requiredMajor) {
        try {
            String name = MultiRTUtils.getExactJreName(requiredMajor);
            if (name == null) {
                name = MultiRTUtils.getNearestJreName(requiredMajor);
            }
            if (name == null) return null;

            // Do not call getRuntimeHome() — it throws if release file is broken
            File home = new File(Tools.MULTIRT_HOME, name);
            if (!home.isDirectory()) return null;
            File java = findJavaBinary(home);
            if (java != null && java.exists()) {
                return java.getAbsolutePath();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String resolveForMinecraftVersion(String mcVersion) {
        int major = CsServerJavaResolver.requiredMajor(mcVersion);
        String path = resolveJavaBin(major);
        if (path != null) return path;
        // Fallback: any installed higher/equal runtime
        try {
            for (Runtime r : MultiRTUtils.getInstalledRuntimes()) {
                if (r == null || r.javaVersion <= 0) continue;
                if (r.javaVersion < major) continue;
                File home = new File(Tools.MULTIRT_HOME, r.name);
                File java = findJavaBinary(home);
                if (java != null && java.exists()) return java.getAbsolutePath();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static String missingRuntimeMessage(String mcVersion) {
        int major = CsServerJavaResolver.requiredMajor(mcVersion);
        return "Java " + major + " runtime not installed. Open MultiRT / Runtime Manager and install Java "
                + major + " first (needed for MC " + mcVersion + ").";
    }

    private static File findJavaBinary(File runtimeHome) {
        if (runtimeHome == null || !runtimeHome.isDirectory()) return null;
        String[] candidates = {
                "bin/java",
                "jre/bin/java",
                "bin/java.exe"
        };
        for (String rel : candidates) {
            File f = new File(runtimeHome, rel);
            if (f.exists()) return f;
        }
        File[] kids = runtimeHome.listFiles();
        if (kids != null) {
            for (File k : kids) {
                if (!k.isDirectory()) continue;
                File bin = new File(k, "bin/java");
                if (bin.exists()) return bin;
            }
        }
        return null;
    }

    public static boolean hasRuntimeFor(String mcVersion) {
        return resolveForMinecraftVersion(mcVersion) != null;
    }

    public static java.util.List<String> installedJavaMajors() {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        try {
            for (Runtime r : MultiRTUtils.getInstalledRuntimes()) {
                if (r != null && r.javaVersion > 0) {
                    out.add(r.name + " (Java " + r.javaVersion + ")");
                }
            }
        } catch (Throwable ignored) {}
        return out;
    }
}
