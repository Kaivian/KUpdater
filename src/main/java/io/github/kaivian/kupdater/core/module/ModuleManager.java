package io.github.kaivian.kupdater.core.module;

import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry and lifecycle manager for KUpdater gameplay enhancement modules.
 */
public class ModuleManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<String, KModule> modules = new LinkedHashMap<>();

    public ModuleManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Registers a module with the manager.
     *
     * @param module module instance to register
     */
    public void registerModule(KModule module) {
        if (modules.containsKey(module.getId())) {
            logger.warning("Module with ID '" + module.getId() + "' is already registered. Skipping.");
            return;
        }
        modules.put(module.getId(), module);
        logger.info("Registered module: " + module.getName() + " [" + module.getId() + "]");
    }

    /**
     * Enables all registered modules.
     */
    public void enableModules() {
        int enabledCount = 0;
        for (KModule module : modules.values()) {
            try {
                module.onEnable();
                module.setEnabled(true);
                enabledCount++;
                logger.info("Enabled module: " + module.getName());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to enable module '" + module.getId() + "'", e);
            }
        }
        logger.info("Successfully enabled " + enabledCount + "/" + modules.size() + " registered modules.");
    }

    /**
     * Disables all registered modules.
     */
    public void disableModules() {
        for (KModule module : modules.values()) {
            if (module.isEnabled()) {
                try {
                    module.onDisable();
                    module.setEnabled(false);
                    logger.info("Disabled module: " + module.getName());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error disabling module '" + module.getId() + "'", e);
                }
            }
        }
    }

    /**
     * Gets a registered module by its ID.
     *
     * @param id unique module identifier
     * @return Optional containing the module if found
     */
    public Optional<KModule> getModule(String id) {
        return Optional.ofNullable(modules.get(id));
    }

    /**
     * Gets an unmodifiable list of all registered modules.
     *
     * @return list of modules
     */
    public List<KModule> getModules() {
        return Collections.unmodifiableList(new ArrayList<>(modules.values()));
    }

    /**
     * Gets all registered modules belonging to a specific category.
     *
     * @param category module category
     * @return list of matching modules
     */
    public List<KModule> getModulesByCategory(ModuleCategory category) {
        List<KModule> result = new ArrayList<>();
        for (KModule module : modules.values()) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Unregisters all modules.
     */
    public void clear() {
        disableModules();
        modules.clear();
    }
}
