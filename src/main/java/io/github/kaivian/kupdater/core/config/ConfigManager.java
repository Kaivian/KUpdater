package io.github.kaivian.kupdater.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

/**
 * Manages configuration loading and saving for KUpdater and its modules.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private final Logger logger;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Initializes default configuration files.
     */
    public void setup() {
        plugin.saveDefaultConfig();
        logger.info("Configuration system initialized.");
    }

    /**
     * Gets the main plugin configuration.
     *
     * @return FileConfiguration instance
     */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /**
     * Reloads configuration from disk.
     */
    public void reload() {
        plugin.reloadConfig();
        logger.info("Configuration reloaded.");
    }

    /**
     * Obtains a module-specific configuration file directory.
     *
     * @param moduleId identifier of the module
     * @return module configuration directory File
     */
    public File getModuleConfigDirectory(String moduleId) {
        File moduleDir = new File(plugin.getDataFolder(), "modules/" + moduleId);
        if (!moduleDir.exists()) {
            moduleDir.mkdirs();
        }
        return moduleDir;
    }
}
