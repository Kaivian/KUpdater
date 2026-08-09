package io.github.kaivian.kupdater.testing.model;

/**
 * Classifies automated test failure types for precise CI diagnostics.
 */
public enum FailureCategory {
    NONE("No failure occurred"),
    UNIT_FAILURE("Unit test assertion failure"),
    INTEGRATION_FAILURE("Integration test execution failure"),
    MINECRAFT_STARTUP_FAILURE("Minecraft server failed to reach ready state within timeout"),
    PLUGIN_LOAD_FAILURE("Plugin loading error or uncaught exception during startup"),
    VERSION_COMPATIBILITY_FAILURE("Version adapter or version range compatibility check failed"),
    DATABASE_FAILURE("Database migration or persistence pool error"),
    MODULE_INITIALIZATION_FAILURE("Unexpected failure during module initialization"),
    SHUTDOWN_FAILURE("Server process failed to shutdown cleanly");

    private final String description;

    FailureCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
