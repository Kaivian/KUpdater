package io.github.kaivian.kupdater.testing.integration;

import io.github.kaivian.kupdater.api.module.AbstractKModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;
import io.github.kaivian.kupdater.api.module.ModuleState;
import io.github.kaivian.kupdater.api.version.MinecraftVersion;
import io.github.kaivian.kupdater.core.module.ModuleManager;
import io.github.kaivian.kupdater.features.combat.CombatModule;
import io.github.kaivian.kupdater.features.exploration.ExplorationModule;
import io.github.kaivian.kupdater.features.farming.FarmingModule;
import io.github.kaivian.kupdater.features.mining.MiningModule;
import io.github.kaivian.kupdater.features.progression.ProgressionModule;
import io.github.kaivian.kupdater.features.tools.ToolsModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ModuleManagerIntegrationTest {

    private ModuleManager moduleManager;

    @BeforeEach
    void setUp() {
        moduleManager = new ModuleManager(null, null, null);
    }

    @Test
    @DisplayName("Should evaluate per-module version compatibility ranges cleanly on Minecraft 1.8.8")
    void testVersionRangeFilteringOn1_8_8() {
        moduleManager.registerModule(new FarmingModule());      // 1.8.8 -> 26.2
        moduleManager.registerModule(new MiningModule());       // 1.8.8 -> 26.2
        moduleManager.registerModule(new ToolsModule());        // 1.12 -> 26.2
        moduleManager.registerModule(new ExplorationModule());  // 1.16 -> 26.2
        moduleManager.registerModule(new CombatModule());       // 1.20 -> 26.2

        moduleManager.enableModules("1.8.8");

        assertEquals(ModuleState.ENABLED, moduleManager.getModule("farming").get().getLifecycleState());
        assertEquals(ModuleState.ENABLED, moduleManager.getModule("mining").get().getLifecycleState());

        assertEquals(ModuleState.DISABLED_UNSUPPORTED_VERSION, moduleManager.getModule("tools").get().getLifecycleState());
        assertEquals(ModuleState.DISABLED_UNSUPPORTED_VERSION, moduleManager.getModule("exploration").get().getLifecycleState());
        assertEquals(ModuleState.DISABLED_UNSUPPORTED_VERSION, moduleManager.getModule("combat").get().getLifecycleState());

        assertFalse(moduleManager.getModule("tools").get().isEnabled());
        assertFalse(moduleManager.getModule("combat").get().isEnabled());
    }

    @Test
    @DisplayName("Should enable all compatible modules on Minecraft 1.21.4")
    void testVersionRangeFilteringOn1_21_4() {
        moduleManager.registerModule(new FarmingModule());
        moduleManager.registerModule(new MiningModule());
        moduleManager.registerModule(new ToolsModule());
        moduleManager.registerModule(new ProgressionModule());
        moduleManager.registerModule(new CombatModule());
        moduleManager.registerModule(new ExplorationModule());

        moduleManager.enableModules("1.21.4");

        assertTrue(moduleManager.getModules().stream().allMatch(m -> m.getLifecycleState() == ModuleState.ENABLED));
        assertTrue(moduleManager.getModules().stream().allMatch(m -> m.isEnabled()));
    }

    @Test
    @DisplayName("Should isolate faulty module initialization without crashing other modules")
    void testFaultIsolationOnModuleFailure() {
        moduleManager.registerModule(new FarmingModule());
        moduleManager.registerModule(new FaultyModule());
        moduleManager.registerModule(new MiningModule());

        moduleManager.enableModules("1.21.4");

        assertEquals(ModuleState.ENABLED, moduleManager.getModule("farming").get().getLifecycleState());
        assertEquals(ModuleState.ENABLED, moduleManager.getModule("mining").get().getLifecycleState());

        assertEquals(ModuleState.FAILED_INITIALIZATION, moduleManager.getModule("faulty").get().getLifecycleState());
        assertFalse(moduleManager.getModule("faulty").get().isEnabled());
    }

    @Test
    @DisplayName("Should disable persistence-requiring module when database is unavailable")
    void testPersistenceRequirementCheck() {
        AbstractKModule persistenceModule = new AbstractKModule("persistent", "Persistent", "Desc",
                ModuleCategory.TOOLS, MinecraftVersion.V1_8_8, MinecraftVersion.V26_2) {
            @Override public void onEnable() {}
            @Override public void onDisable() {}
            @Override public boolean requiresPersistence() { return true; }
        };

        moduleManager.registerModule(persistenceModule);
        moduleManager.enableModules("1.21.4");

        assertEquals(ModuleState.DISABLED_PERSISTENCE_UNAVAILABLE, persistenceModule.getLifecycleState());
        assertFalse(persistenceModule.isEnabled());
    }

    private static class FaultyModule extends AbstractKModule {
        protected FaultyModule() {
            super("faulty", "Faulty Module", "Desc", ModuleCategory.COMBAT);
        }

        @Override
        public void onEnable() {
            throw new RuntimeException("Simulated unexpected module initialization crash!");
        }

        @Override
        public void onDisable() {
        }
    }
}
