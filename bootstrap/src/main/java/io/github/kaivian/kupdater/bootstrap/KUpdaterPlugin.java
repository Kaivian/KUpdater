package io.github.kaivian.kupdater.bootstrap;

import io.github.kaivian.kupdater.core.config.ConfigManager;
import io.github.kaivian.kupdater.core.database.DatabaseManager;
import io.github.kaivian.kupdater.core.module.ModuleManager;
import io.github.kaivian.kupdater.features.combat.CombatModule;
import io.github.kaivian.kupdater.features.economy.EconomyModule;
import io.github.kaivian.kupdater.features.exploration.ExplorationModule;
import io.github.kaivian.kupdater.features.farming.FarmingModule;
import io.github.kaivian.kupdater.features.mining.MiningModule;
import io.github.kaivian.kupdater.features.progression.ProgressionModule;
import io.github.kaivian.kupdater.features.tools.ToolsModule;
import io.github.kaivian.kupdater.platform.common.PlatformManager;
import io.github.kaivian.kupdater.platform.v1_13.Paper1_13Adapter;
import io.github.kaivian.kupdater.platform.v1_21.Paper1_21Adapter;
import io.github.kaivian.kupdater.platform.v1_8.Paper1_8Adapter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main JavaPlugin entrypoint and assembly bootstrapper for KUpdater.
 */
public final class KUpdaterPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ModuleManager moduleManager;
    private PlatformManager platformManager;

    @Override
    public void onEnable() {
        getLogger().info("Initializing KUpdater modular framework...");

        // Initialize platform manager and version adapters across support range (1.8.8 -> 26.2)
        this.platformManager = new PlatformManager(getLogger());
        this.platformManager.registerAdapter(new Paper1_8Adapter());
        this.platformManager.registerAdapter(new Paper1_13Adapter());
        this.platformManager.registerAdapter(new Paper1_21Adapter());
        this.platformManager.initialize(getServer().getVersion());

        // Initialize core config system
        this.configManager = new ConfigManager(this);
        this.configManager.setup();

        // Initialize database persistence manager
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.setup();

        // Initialize module manager with persistence and config support
        this.moduleManager = new ModuleManager(this, this.databaseManager, this.configManager);

        // Register feature module skeletons
        registerFeatureModules();

        // Enable registered modules against current server version
        this.moduleManager.enableModules(getServer().getVersion());

        getLogger().info("KUpdater startup completed successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down KUpdater...");

        if (this.moduleManager != null) {
            this.moduleManager.disableModules();
        }

        if (this.databaseManager != null) {
            this.databaseManager.shutdown();
        }

        getLogger().info("KUpdater shutdown completed.");
    }

    private void registerFeatureModules() {
        moduleManager.registerModule(new ToolsModule());
        moduleManager.registerModule(new ProgressionModule());
        moduleManager.registerModule(new CombatModule());
        moduleManager.registerModule(new FarmingModule());
        moduleManager.registerModule(new MiningModule());
        moduleManager.registerModule(new EconomyModule());
        moduleManager.registerModule(new ExplorationModule());
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public PlatformManager getPlatformManager() {
        return platformManager;
    }
}

