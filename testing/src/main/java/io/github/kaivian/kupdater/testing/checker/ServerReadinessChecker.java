package io.github.kaivian.kupdater.testing.checker;

import io.github.kaivian.kupdater.api.module.ModuleState;
import io.github.kaivian.kupdater.testing.model.FailureCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Programmatically inspects server log output lines for plugin lifecycle assertion verification
 * and failure pattern identification.
 */
public class ServerReadinessChecker {

    private final List<String> logLines;

    public ServerReadinessChecker(List<String> logLines) {
        this.logLines = logLines != null ? logLines : List.of();
    }

    public boolean hasPluginEnabledMarker() {
        return containsText("KUpdater startup completed successfully.");
    }

    public boolean hasPluginShutdownMarker() {
        return containsText("KUpdater shutdown completed.");
    }

    public FailureCategory detectFailureCategory() {
        if (containsText("ClassNotFoundException") || containsText("NoClassDefFoundError") || containsText("PluginException")) {
            return FailureCategory.PLUGIN_LOAD_FAILURE;
        }
        if (containsText("MigrationException") || containsText("HikariPool") || containsText("DatabaseManager")) {
            return FailureCategory.DATABASE_FAILURE;
        }
        if (containsText("FAILED_INITIALIZATION") || containsText("failed to initialize cleanly")) {
            return FailureCategory.MODULE_INITIALIZATION_FAILURE;
        }
        return FailureCategory.NONE;
    }

    public Map<String, ModuleState> parseModuleStates(List<String> registeredModuleIds) {
        Map<String, ModuleState> map = new HashMap<>();
        if (registeredModuleIds == null) return map;

        for (String id : registeredModuleIds) {
            ModuleState detectedState = ModuleState.DISCOVERED;
            for (String line : logLines) {
                if (line.contains("[" + id + "]")) {
                    if (line.contains("Enabled module:")) {
                        detectedState = ModuleState.ENABLED;
                    } else if (line.contains("Disabled in configuration")) {
                        detectedState = ModuleState.DISABLED_BY_CONFIGURATION;
                    } else if (line.contains("Unsupported Minecraft version")) {
                        detectedState = ModuleState.DISABLED_UNSUPPORTED_VERSION;
                    } else if (line.contains("Database persistence required but unavailable")) {
                        detectedState = ModuleState.DISABLED_PERSISTENCE_UNAVAILABLE;
                    } else if (line.contains("failed to initialize cleanly")) {
                        detectedState = ModuleState.FAILED_INITIALIZATION;
                    }
                }
            }
            map.put(id, detectedState);
        }
        return map;
    }

    public boolean containsText(String subText) {
        for (String line : logLines) {
            if (line.contains(subText)) {
                return true;
            }
        }
        return false;
    }
}
