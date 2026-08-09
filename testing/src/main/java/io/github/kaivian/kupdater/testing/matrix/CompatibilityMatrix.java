package io.github.kaivian.kupdater.testing.matrix;

import io.github.kaivian.kupdater.api.version.MinecraftVersion;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Single source of truth for version matrix configuration and version alias resolution.
 */
public final class CompatibilityMatrix {

    public static final String DEFAULT_PRIMARY_VERSION = "1.21.4";
    public static final List<String> DEFAULT_SUPPORTED_VERSIONS = Collections.unmodifiableList(
            Arrays.asList("1.8.8", "1.12.2", "1.16.5", "1.20.4", "1.21.4")
    );

    private final String primaryVersion;
    private final List<String> supportedVersions;
    private final int startupTimeoutSeconds;
    private final int shutdownTimeoutSeconds;
    private final int testTimeoutSeconds;

    public CompatibilityMatrix() {
        this(DEFAULT_PRIMARY_VERSION, DEFAULT_SUPPORTED_VERSIONS, 120, 30, 180);
    }

    public CompatibilityMatrix(String primaryVersion, List<String> supportedVersions,
                               int startupTimeoutSeconds, int shutdownTimeoutSeconds, int testTimeoutSeconds) {
        this.primaryVersion = primaryVersion != null ? primaryVersion : DEFAULT_PRIMARY_VERSION;
        this.supportedVersions = supportedVersions != null && !supportedVersions.isEmpty()
                ? Collections.unmodifiableList(new ArrayList<>(supportedVersions))
                : DEFAULT_SUPPORTED_VERSIONS;
        this.startupTimeoutSeconds = startupTimeoutSeconds;
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
        this.testTimeoutSeconds = testTimeoutSeconds;
    }

    public static CompatibilityMatrix loadDefault() {
        return new CompatibilityMatrix();
    }

    public String getPrimaryVersion() {
        return primaryVersion;
    }

    public List<String> getSupportedVersions() {
        return supportedVersions;
    }

    public String getOldestSupportedVersion() {
        return supportedVersions.get(0);
    }

    public String getLatestSupportedVersion() {
        return supportedVersions.get(supportedVersions.size() - 1);
    }

    public int getStartupTimeoutSeconds() {
        return startupTimeoutSeconds;
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    public int getTestTimeoutSeconds() {
        return testTimeoutSeconds;
    }

    /**
     * Resolves version level alias ("full", "smoke", "boundary", "primary").
     *
     * @param level level name
     * @return list of Minecraft version strings
     */
    public List<String> resolveLevel(String level) {
        if (level == null) {
            return Collections.singletonList(primaryVersion);
        }

        switch (level.toLowerCase().trim()) {
            case "primary":
            case "full":
                return Collections.singletonList(primaryVersion);
            case "smoke":
                List<String> smokeList = new ArrayList<>();
                smokeList.add(getOldestSupportedVersion());
                if (!smokeList.contains(primaryVersion)) smokeList.add(primaryVersion);
                if (!smokeList.contains(getLatestSupportedVersion())) smokeList.add(getLatestSupportedVersion());
                return smokeList;
            case "boundary":
            case "all":
            case "compatibility":
                return supportedVersions;
            default:
                return Collections.singletonList(level);
        }
    }
}
