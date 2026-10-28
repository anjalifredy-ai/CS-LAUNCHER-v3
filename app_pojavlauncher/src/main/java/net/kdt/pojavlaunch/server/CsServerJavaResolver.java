package net.kdt.pojavlaunch.server;

/**
 * Maps Minecraft server version → required major Java version (8 / 17 / 21).
 */
public final class CsServerJavaResolver {

    public enum JavaMajor {
        JAVA_8(8, "Java 8"),
        JAVA_17(17, "Java 17"),
        JAVA_21(21, "Java 21");

        public final int major;
        public final String label;

        JavaMajor(int major, String label) {
            this.major = major;
            this.label = label;
        }
    }

    private CsServerJavaResolver() {}

    public static JavaMajor requiredJava(String minecraftVersion) {
        if (minecraftVersion == null || minecraftVersion.isEmpty()) {
            return JavaMajor.JAVA_17;
        }
        String[] parts = minecraftVersion.split("\\.");
        int major = 1;
        int minor = 0;
        try {
            if (parts.length > 0) major = Integer.parseInt(parts[0]);
            if (parts.length > 1) minor = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {}

        if (major >= 2) return JavaMajor.JAVA_21;
        if (minor >= 20) return JavaMajor.JAVA_21; // 1.20+
        if (minor >= 17) return JavaMajor.JAVA_17; // 1.17–1.19
        return JavaMajor.JAVA_8; // 1.16 and below
    }

    public static JavaMajor[] availableRuntimes() {
        return JavaMajor.values();
    }
}
