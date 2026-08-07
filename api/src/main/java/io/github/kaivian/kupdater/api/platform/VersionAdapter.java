package io.github.kaivian.kupdater.api.platform;

/**
 * Abstraction for platform-specific and Minecraft version-dependent features.
 */
public interface VersionAdapter {

    /**
     * Gets the target Minecraft version string for this adapter (e.g., "1.21.x").
     *
     * @return supported Minecraft version identifier
     */
    String getTargetVersion();

    /**
     * Checks if this adapter is compatible with the running server environment.
     *
     * @param serverVersion current server version string from Bukkit
     * @return true if compatible, false otherwise
     */
    boolean isCompatible(String serverVersion);
}
