package io.github.kaivian.kupdater.platform.common;

import io.github.kaivian.kupdater.api.platform.VersionAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Manages platform compatibility adapters for different Minecraft server versions.
 */
public class PlatformManager {

    private final Logger logger;
    private final List<VersionAdapter> registeredAdapters = new ArrayList<>();
    private VersionAdapter activeAdapter;

    public PlatformManager(Logger logger) {
        this.logger = logger;
    }

    /**
     * Registers a version adapter implementation.
     *
     * @param adapter VersionAdapter instance
     */
    public void registerAdapter(VersionAdapter adapter) {
        registeredAdapters.add(adapter);
        logger.info("Registered platform adapter for version target: " + adapter.getTargetVersion());
    }

    /**
     * Resolves and activates the appropriate adapter based on current server version.
     *
     * @param serverVersion Minecraft version string from server runtime
     * @return true if a compatible adapter was selected, false otherwise
     */
    public boolean initialize(String serverVersion) {
        for (VersionAdapter adapter : registeredAdapters) {
            if (adapter.isCompatible(serverVersion)) {
                this.activeAdapter = adapter;
                logger.info("Activated version adapter for: " + adapter.getTargetVersion());
                return true;
            }
        }
        logger.warning("No specific version adapter matched version '" + serverVersion + "'. Running with default fallback.");
        return false;
    }

    /**
     * Gets the currently active version adapter.
     *
     * @return Optional containing the active adapter
     */
    public Optional<VersionAdapter> getActiveAdapter() {
        return Optional.ofNullable(activeAdapter);
    }

    /**
     * Gets an unmodifiable list of all registered version adapters.
     *
     * @return list of registered adapters
     */
    public List<VersionAdapter> getRegisteredAdapters() {
        return Collections.unmodifiableList(registeredAdapters);
    }
}
