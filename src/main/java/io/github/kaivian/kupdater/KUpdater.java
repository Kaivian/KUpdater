package io.github.kaivian.kupdater;

import io.github.kaivian.kupdater.core.config.ConfigManager;
import io.github.kaivian.kupdater.core.module.ModuleManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class KUpdater extends JavaPlugin {

    private ConfigManager configManager;
    private ModuleManager moduleManager;

    @Override
    public void onEnable() {
        getLogger().info("Initializing KUpdater foundation...");

        // Initialize core systems
        this.configManager = new ConfigManager(this);
        this.configManager.setup();

        this.moduleManager = new ModuleManager(this);

        // Enable registered modules
        this.moduleManager.enableModules();

        getLogger().info("KUpdater successfully enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down KUpdater...");

        if (this.moduleManager != null) {
            this.moduleManager.disableModules();
        }

        getLogger().info("KUpdater successfully disabled.");
    }

    /**
     * Gets the configuration manager instance.
     *
     * @return ConfigManager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the module manager instance.
     *
     * @return ModuleManager
     */
    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}
