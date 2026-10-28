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

    /**
     * @param requiredMajor 8, 17, or 21
     * @return absolute path to java binary, or null if no suitable runtime installed
     */
    public static String resolveJavaBin(int requiredMajor) {
        String name = MultiRTUtils.getExactJreName(requiredMajor);
        if (name == null) {
            name = MultiRTUtils.getNearestJreName(requiredMajor);
        }
        if (name == null) return null;

        try {
            File home = MultiRTUtils.getRuntimeHome(name);
            File java = findJavaBinary(home);
            if (java != null && java.canExecute()) {
                return java.getAbsolutePath();
            }
        } catch (Exception e) {
            // broken runtime
        }
        return null;
    }

    public static String resolveForMinecraftVersion(String mcVersion) {
        int major = CsServerJavaResolver.requiredMajor(mcVersion);
        return resolveJavaBin(major);
    }

    public static String missingRuntimeMessage(String mcVersion) {
        int major = CsServerJavaResolver.requiredMajor(mcVersion);
        return "Java " + major + " runtime not installed. Open MultiRT / Runtime Manager and install Java "
                + major + " first (needed for MC " + mcVersion + ").";
    }

    private static File findJavaBinary(File runtimeHome) {
        // Common MultiRT layouts
        String[] candidates = {
                "bin/java",
                "jre/bin/java",
                "bin/java.exe"
        };
        for (String rel : candidates) {
            File f = new File(runtimeHome, rel);
            if (f.exists()) return f;
        }
        // Walk one level for nested jre-
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
        for (Runtime r : MultiRTUtils.getInstalledRuntimes()) {
            if (r.javaVersion > 0) {
                out.add(r.name + " (Java " + r.javaVersion + ")");
            }
        }
        return out;
    }
}
