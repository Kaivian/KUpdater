package io.github.kaivian.kupdater.api.module;

/**
 * Represents the lifecycle state of a KUpdater gameplay module.
 */
public enum ModuleState {
    /**
     * Module instance registered with ModuleManager.
     */
    DISCOVERED(false),

    /**
     * Checking module enable/disable settings in configuration.
     */
    CONFIG_CHECK(false),

    /**
     * Evaluating server version compatibility against module supported version range.
     */
    VERSION_CHECK(false),

    /**
     * Checking external requirements (e.g., database persistence availability).
     */
    REQUIREMENTS_CHECK(false),

    /**
     * Executing module setup/onEnable logic.
     */
    INITIALIZING(false),

    /**
     * Module successfully initialized and active.
     */
    ENABLED(true),

    /**
     * Module disabled via configuration setting (modules.<id>.enabled = false).
     */
    DISABLED_BY_CONFIGURATION(false),

    /**
     * Module disabled because current server version is outside supported version range.
     */
    DISABLED_UNSUPPORTED_VERSION(false),

    /**
     * Module disabled because required database persistence is unavailable.
     */
    DISABLED_PERSISTENCE_UNAVAILABLE(false),

    /**
     * Module failed to enable due to an unexpected exception during initialization.
     */
    FAILED_INITIALIZATION(false),

    /**
     * Module is executing onDisable logic.
     */
    DISABLING(false),

    /**
     * Module disabled cleanly.
     */
    DISABLED(false);

    private final boolean active;

    ModuleState(boolean active) {
        this.active = active;
    }

    /**
     * Checks if this state represents an active enabled module.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isActive() {
        return active;
    }
}
