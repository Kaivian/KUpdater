package io.github.kaivian.kupdater.core.database.module;

import io.github.kaivian.kupdater.api.database.PersistenceState;
import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;
import io.github.kaivian.kupdater.core.database.DatabaseManager;
import io.github.kaivian.kupdater.core.module.ModuleManager;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModuleManagerPersistenceTest {

    static class DummyModule implements KModule {
        private final String id;
        private final boolean requiresPersistence;
        private boolean enabled = false;

        DummyModule(String id, boolean requiresPersistence) {
            this.id = id;
            this.requiresPersistence = requiresPersistence;
        }

        @Override
        public String getId() { return id; }

        @Override
        public String getName() { return id; }

        @Override
        public String getDescription() { return id; }

        @Override
        public ModuleCategory getCategory() { return ModuleCategory.OTHER; }

        @Override
        public List<String> getSupportedMinecraftVersions() { return Collections.singletonList("*"); }

        @Override
        public void onEnable() { this.enabled = true; }

        @Override
        public void onDisable() { this.enabled = false; }

        @Override
        public boolean isEnabled() { return enabled; }

        @Override
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        @Override
        public boolean requiresPersistence() { return requiresPersistence; }
    }

    static class TestDatabaseManager extends DatabaseManager {
        private final boolean available;
        private final PersistenceState mockState;

        TestDatabaseManager(boolean available, PersistenceState mockState) {
            super(null);
            this.available = available;
            this.mockState = mockState;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public PersistenceState getState() {
            return mockState;
        }
    }

    @Test
    void testModuleSkippedWhenPersistenceDisabled() {
        DatabaseManager dbManager = new TestDatabaseManager(false, PersistenceState.DISABLED);
        ModuleManager moduleManager = new ModuleManager(null, dbManager);

        DummyModule standardModule = new DummyModule("standard", false);
        DummyModule persistenceModule = new DummyModule("db-dependent", true);

        moduleManager.registerModule(standardModule);
        moduleManager.registerModule(persistenceModule);

        moduleManager.enableModules();

        assertTrue(standardModule.isEnabled(), "Module that does not require persistence should be enabled");
        assertFalse(persistenceModule.isEnabled(), "Module that requires persistence should be skipped when persistence is unavailable");
    }

    @Test
    void testModuleEnabledWhenPersistenceAvailable() {
        DatabaseManager dbManager = new TestDatabaseManager(true, PersistenceState.AVAILABLE);
        ModuleManager moduleManager = new ModuleManager(null, dbManager);

        DummyModule persistenceModule = new DummyModule("db-dependent", true);
        moduleManager.registerModule(persistenceModule);

        moduleManager.enableModules();

        assertTrue(persistenceModule.isEnabled(), "Module that requires persistence should be enabled when persistence is available");
    }
}
