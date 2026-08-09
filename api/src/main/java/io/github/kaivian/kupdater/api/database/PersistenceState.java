package io.github.kaivian.kupdater.api.database;

/**
 * Represents the lifecycle state of the KUpdater persistence infrastructure.
 */
public enum PersistenceState {
    /**
     * Database persistence is explicitly disabled via configuration.
     */
    DISABLED,

    /**
     * Database persistence is currently initializing (validating configuration, establishing connections, running migrations).
     */
    INITIALIZING,

    /**
     * Database persistence is fully initialized, connected, migrated, and ready for operations.
     */
    AVAILABLE,

    /**
     * Database persistence failed to initialize or encountered an unrecoverable connectivity error.
     */
    FAILED,

    /**
     * Database persistence has been shut down cleanly during plugin disable.
     */
    SHUTDOWN
}
