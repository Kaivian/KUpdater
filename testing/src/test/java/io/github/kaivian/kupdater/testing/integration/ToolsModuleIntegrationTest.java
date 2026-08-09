package io.github.kaivian.kupdater.testing.integration;

import io.github.kaivian.kupdater.api.database.DatabaseConfiguration;
import io.github.kaivian.kupdater.api.database.DatabaseType;
import io.github.kaivian.kupdater.api.module.ModuleState;
import io.github.kaivian.kupdater.core.database.DatabaseManager;
import io.github.kaivian.kupdater.core.module.ModuleManager;
import io.github.kaivian.kupdater.features.tools.ToolsModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ToolsModuleIntegrationTest {

    private DatabaseManager databaseManager;
    private ModuleManager moduleManager;
    private File tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = File.createTempFile("kupdater_tools_mod_test_", "_dir");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();

        databaseManager = new DatabaseManager(null);
    }

    @AfterEach
    void tearDown() {
        if (databaseManager != null && databaseManager.isAvailable()) {
            databaseManager.shutdown();
        }
    }

    @Test
    @DisplayName("Should disable ToolsModule when database persistence is required but unavailable")
    void testDisableWhenPersistenceUnavailable() {
        moduleManager = new ModuleManager(null, null, null);
        ToolsModule toolsModule = new ToolsModule(null, null, null);
        moduleManager.registerModule(toolsModule);

        moduleManager.enableModules("1.21.4");

        assertEquals(ModuleState.DISABLED_PERSISTENCE_UNAVAILABLE, toolsModule.getLifecycleState());
        assertFalse(toolsModule.isEnabled());
    }

    @Test
    @DisplayName("Should disable ToolsModule on unsupported Minecraft versions (e.g. 1.8.8)")
    void testDisableOnUnsupportedVersion() {
        moduleManager = new ModuleManager(null, databaseManager, null);
        ToolsModule toolsModule = new ToolsModule(null, databaseManager, null);
        moduleManager.registerModule(toolsModule);

        moduleManager.enableModules("1.8.8");

        assertEquals(ModuleState.DISABLED_UNSUPPORTED_VERSION, toolsModule.getLifecycleState());
        assertFalse(toolsModule.isEnabled());
    }
}
