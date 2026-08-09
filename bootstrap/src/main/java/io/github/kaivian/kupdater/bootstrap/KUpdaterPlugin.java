package io.github.kaivian.kupdater.bootstrap;

import io.github.kaivian.kupdater.core.config.ConfigManager;
import io.github.kaivian.kupdater.core.module.ModuleManager;
import io.github.kaivian.kupdater.features.combat.CombatModule;
import io.github.kaivian.kupdater.features.economy.EconomyModule;
import io.github.kaivian.kupdater.features.exploration.ExplorationModule;
import io.github.kaivian.kupdater.features.farming.FarmingModule;
import io.github.kaivian.kupdater.features.mining.MiningModule;
import io.github.kaivian.kupdater.features.progression.ProgressionModule;
import io.github.kaivian.kupdater.features.tools.ToolsModule;
import io.github.kaivian.kupdater.platform.common.PlatformManager;
import io.github.kaivian.kupdater.platform.v1_21.Paper1_21Adapter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main JavaPlugin entrypoint and assembly bootstrapper for KUpdater.
 */
public final class KUpdaterPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ModuleManager moduleManager;
    private PlatformManager platformManager;

    @Override
    public void onEnable() {
        getLogger().info("Initializing KUpdater modular framework...");

        // Initialize platform manager and version adapter
        this.platformManager = new PlatformManager(getLogger());
        this.platformManager.registerAdapter(new Paper1_21Adapter());
        this.platformManager.initialize(getServer().getVersion());

        // Initialize core systems
        this.configManager = new ConfigManager(this);
        this.configManager.setup();

        this.moduleManager = new ModuleManager(this);

        // Register feature module skeletons
        registerFeatureModules();

        // Enable registered modules
        this.moduleManager.enableModules();

        getLogger().info("KUpdater successfully enabled with modular architecture.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down KUpdater...");

        if (this.moduleManager != null) {
            this.moduleManager.disableModules();
        }

        getLogger().info("KUpdater successfully disabled.");
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

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public PlatformManager getPlatformManager() {
        return platformManager;
    }
}
