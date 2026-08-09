package io.github.kaivian.kupdater.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

/**
 * Manages configuration loading, module config isolation, and saving for KUpdater and its modules.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private final Logger logger;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger("ConfigManager");
    }

    /**
     * Initializes default configuration files.
     */
    public void setup() {
        if (plugin != null) {
            plugin.saveDefaultConfig();
        }
        logger.info("Configuration system initialized.");
    }

    /**
     * Gets the main plugin configuration.
     *
     * @return FileConfiguration instance
     */
    public FileConfiguration getConfig() {
        return plugin != null ? plugin.getConfig() : null;
    }

    /**
     * Reloads configuration from disk.
     */
    public void reload() {
        if (plugin != null) {
            plugin.reloadConfig();
        }
        logger.info("Configuration reloaded.");
    }

    /**
     * Obtains a module-specific configuration file directory (plugins/KUpdater/modules/<moduleId>).
     *
     * @param moduleId identifier of the module
     * @return module configuration directory File
     */
    public File getModuleConfigDirectory(String moduleId) {
        File dataFolder = plugin != null ? plugin.getDataFolder() : new File("plugins/KUpdater");
        File moduleDir = new File(dataFolder, "modules/" + moduleId);
        if (!moduleDir.exists()) {
            moduleDir.mkdirs();
        }
        return moduleDir;
    }

    /**
     * Loads a dedicated module configuration file (e.g. plugins/KUpdater/modules/tools.yml).
     * If the file does not exist, copies defaultResourceName from embedded plugin resources.
     *
     * @param moduleId identifier of the module (e.g. "tools")
     * @param defaultResourceName default resource path inside jar (e.g. "tools.yml")
     * @return FileConfiguration instance
     */
    public FileConfiguration loadModuleConfig(String moduleId, String defaultResourceName) {
        if (plugin == null) return null;

        File modulesDir = new File(plugin.getDataFolder(), "modules");
        if (!modulesDir.exists()) {
            modulesDir.mkdirs();
        }

        File configFile = new File(modulesDir, moduleId + ".yml");
        if (!configFile.exists()) {
            try (InputStream in = plugin.getResource(defaultResourceName)) {
                if (in != null) {
                    Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Created isolated module configuration: modules/" + moduleId + ".yml");
                }
            } catch (Exception e) {
                logger.warning("Could not create default module configuration for " + moduleId + ": " + e.getMessage());
            }
        }

        if (configFile.exists()) {
            return YamlConfiguration.loadConfiguration(configFile);
        }

        return plugin.getConfig();
    }

    /**
     * Loads a specific module configuration file inside plugins/KUpdater/modules/<moduleId>/<configFileName>.
     *
     * @param moduleId identifier of the module (e.g. "tools")
     * @param configFileName name of the config file (e.g. "tool-config.yml" or "pickaxe.yml")
     * @return FileConfiguration instance
     */
    public FileConfiguration loadModuleConfigFile(String moduleId, String configFileName) {
        if (plugin == null) return null;

        File moduleFolder = new File(plugin.getDataFolder(), "modules/" + moduleId);
        if (!moduleFolder.exists()) {
            moduleFolder.mkdirs();
        }

        File configFile = new File(moduleFolder, configFileName);
        if (!configFile.exists()) {
            String resourcePath = moduleId + "/" + configFileName;
            InputStream in = plugin.getResource(resourcePath);
            if (in == null) {
                in = plugin.getResource(configFileName);
            }

            if (in != null) {
                try (InputStream stream = in) {
                    Files.copy(stream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Created isolated module configuration: modules/" + moduleId + "/" + configFileName);
                } catch (Exception e) {
                    logger.warning("Could not create default module configuration for " + moduleId + "/" + configFileName + ": " + e.getMessage());
                }
            }
        }

        if (configFile.exists()) {
            return YamlConfiguration.loadConfiguration(configFile);
        }

        return new YamlConfiguration();
    }

    /**
     * Checks if a module is enabled in global configuration (modules.<id>.enabled).
     * Defaults to true if not specified.
     *
     * @param moduleId identifier of the module
     * @return true if enabled in config, false otherwise
     */
    public boolean isModuleEnabled(String moduleId) {
        if (plugin == null || plugin.getConfig() == null) {
            return true;
        }
        return plugin.getConfig().getBoolean("modules." + moduleId + ".enabled", true);
    }
}
