package io.github.kaivian.kupdater.api.version;

import java.util.Objects;

/**
 * Value object representing a Minecraft server version (e.g., 1.8.8, 1.12.2, 1.20.4, 1.21.4, 26.2).
 */
public final class MinecraftVersion implements Comparable<MinecraftVersion> {

    public static final MinecraftVersion V1_8_8 = of("1.8.8");
    public static final MinecraftVersion V1_12_2 = of("1.12.2");
    public static final MinecraftVersion V1_13 = of("1.13.0");
    public static final MinecraftVersion V1_20_4 = of("1.20.4");
    public static final MinecraftVersion V1_21 = of("1.21.0");
    public static final MinecraftVersion V1_21_4 = of("1.21.4");
    public static final MinecraftVersion V26_2 = of("26.2.0");

    private final String rawVersion;
    private final int major;
    private final int minor;
    private final int patch;

    private MinecraftVersion(String rawVersion, int major, int minor, int patch) {
        this.rawVersion = rawVersion;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Parses a version string into a MinecraftVersion object.
     *
     * @param versionStr version string (e.g. "1.21.4", "1.12", "26.2")
     * @return parsed MinecraftVersion instance
     */
    public static MinecraftVersion of(String versionStr) {
        if (versionStr == null || versionStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Version string cannot be null or empty");
        }

        String cleaned = versionStr.trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?").matcher(cleaned);
        int major = 0;
        int minor = 0;
        int patch = 0;

        if (matcher.find()) {
            try {
                major = Integer.parseInt(matcher.group(1));
                minor = Integer.parseInt(matcher.group(2));
                if (matcher.group(3) != null && !matcher.group(3).isEmpty()) {
                    patch = Integer.parseInt(matcher.group(3));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return new MinecraftVersion(versionStr, major, minor, patch);
    }

    public String getRawVersion() {
        return rawVersion;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    @Override
    public int compareTo(MinecraftVersion o) {
        if (this.major != o.major) {
            return Integer.compare(this.major, o.major);
        }
        if (this.minor != o.minor) {
            return Integer.compare(this.minor, o.minor);
        }
        return Integer.compare(this.patch, o.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinecraftVersion that = (MinecraftVersion) o;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return rawVersion;
    }
}
