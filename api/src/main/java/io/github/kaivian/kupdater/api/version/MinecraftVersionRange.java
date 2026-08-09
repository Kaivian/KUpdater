package io.github.kaivian.kupdater.api.version;

import java.util.Objects;

/**
 * Immutable range object representing a supported Minecraft version interval [minVersion, maxVersion].
 */
public final class MinecraftVersionRange {

    private final MinecraftVersion minVersion;
    private final MinecraftVersion maxVersion;

    private MinecraftVersionRange(MinecraftVersion minVersion, MinecraftVersion maxVersion) {
        this.minVersion = Objects.requireNonNull(minVersion, "minVersion cannot be null");
        this.maxVersion = Objects.requireNonNull(maxVersion, "maxVersion cannot be null");
        if (minVersion.compareTo(maxVersion) > 0) {
            throw new IllegalArgumentException("Minimum version (" + minVersion + ") cannot be greater than maximum version (" + maxVersion + ")");
        }
    }

    public static MinecraftVersionRange of(MinecraftVersion minVersion, MinecraftVersion maxVersion) {
        return new MinecraftVersionRange(minVersion, maxVersion);
    }

    public static MinecraftVersionRange of(String minVersionStr, String maxVersionStr) {
        return new MinecraftVersionRange(MinecraftVersion.of(minVersionStr), MinecraftVersion.of(maxVersionStr));
    }

    public static MinecraftVersionRange single(String versionStr) {
        MinecraftVersion version = MinecraftVersion.of(versionStr);
        return new MinecraftVersionRange(version, version);
    }

    public MinecraftVersion getMinVersion() {
        return minVersion;
    }

    public MinecraftVersion getMaxVersion() {
        return maxVersion;
    }

    public boolean contains(MinecraftVersion version) {
        if (version == null) {
            return false;
        }
        return version.compareTo(minVersion) >= 0 && version.compareTo(maxVersion) <= 0;
    }

    public boolean contains(String versionStr) {
        if (versionStr == null || versionStr.trim().isEmpty()) {
            return false;
        }
        return contains(MinecraftVersion.of(versionStr));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinecraftVersionRange that = (MinecraftVersionRange) o;
        return Objects.equals(minVersion, that.minVersion) && Objects.equals(maxVersion, that.maxVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minVersion, maxVersion);
    }

    @Override
    public String toString() {
        return minVersion.getRawVersion() + " - " + maxVersion.getRawVersion();
    }
}
