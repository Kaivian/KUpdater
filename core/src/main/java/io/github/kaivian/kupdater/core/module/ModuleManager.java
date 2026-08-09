package io.github.kaivian.kupdater.core.module;

import io.github.kaivian.kupdater.api.capability.CapabilityRegistry;
import io.github.kaivian.kupdater.api.database.PersistenceState;
import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;
import io.github.kaivian.kupdater.api.module.ModuleState;
import io.github.kaivian.kupdater.api.version.MinecraftVersion;
import io.github.kaivian.kupdater.core.capability.SimpleCapabilityRegistry;
import io.github.kaivian.kupdater.core.config.ConfigManager;
import io.github.kaivian.kupdater.core.database.DatabaseManager;
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
 * Implements fault isolation and explicit module state machine transitions.
 */
public class ModuleManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final SimpleCapabilityRegistry capabilityRegistry = new SimpleCapabilityRegistry();
    private final Map<String, KModule> modules = new LinkedHashMap<>();

    public ModuleManager(JavaPlugin plugin) {
        this(plugin, null, null);
    }

    public ModuleManager(JavaPlugin plugin, DatabaseManager databaseManager) {
        this(plugin, databaseManager, null);
    }

    public ModuleManager(JavaPlugin plugin, DatabaseManager databaseManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger("ModuleManager");
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }
    /**
     * Registers a module with the manager.
     *
     * @param module module instance to register
     */
    public void registerModule(KModule module) {
        if (module == null) {
            logger.warning("Attempted to register null module.");
            return;
        }

        if (modules.containsKey(module.getId())) {
            logger.warning("Module with ID '" + module.getId() + "' is already registered. Skipping.");
            return;
        }

        module.setLifecycleState(ModuleState.DISCOVERED);
        modules.put(module.getId(), module);
        logger.info("Registered module: " + module.getName() + " [" + module.getId() + "]");
    }

    /**
     * Enables all registered modules using the default running Bukkit server version.
     */
    public void enableModules() {
        String serverVersion = plugin != null && plugin.getServer() != null ? plugin.getServer().getBukkitVersion() : "26.2.0";
        enableModules(serverVersion);
    }

    /**
     * Enables all registered modules against a target server version string.
     *
     * @param serverVersion Minecraft server version string
     */
    public void enableModules(String serverVersion) {
        List<String> validationErrors = ModuleMetadataValidator.validate(modules.values());
        if (!validationErrors.isEmpty()) {
            for (String error : validationErrors) {
                logger.warning("[KUpdater] Module metadata validation warning: " + error);
            }
        }

        MinecraftVersion targetVersion = MinecraftVersion.of(serverVersion);
        int enabledCount = 0;

        for (KModule module : modules.values()) {
            // 1. CONFIG_CHECK
            module.setLifecycleState(ModuleState.CONFIG_CHECK);
            if (configManager != null && !configManager.isModuleEnabled(module.getId())) {
                module.setLifecycleState(ModuleState.DISABLED_BY_CONFIGURATION);
                module.setEnabled(false);
                logger.info("[KUpdater] Module '" + module.getName() + "' [" + module.getId() + "] disabled. Reason: Disabled in configuration.");
                continue;
            }

            // 2. VERSION_CHECK
            module.setLifecycleState(ModuleState.VERSION_CHECK);
            if (!module.supports(targetVersion)) {
                module.setLifecycleState(ModuleState.DISABLED_UNSUPPORTED_VERSION);
                module.setEnabled(false);
                logger.info("[KUpdater] Module '" + module.getName() + "' [" + module.getId() + "] disabled.");
                logger.info("[KUpdater] Reason: Unsupported Minecraft version.");
                logger.info("[KUpdater] Server version: " + targetVersion.getRawVersion());
                logger.info("[KUpdater] Supported versions: " + module.getSupportedVersionRange());
                continue;
            }

            // 3. REQUIREMENTS_CHECK
            module.setLifecycleState(ModuleState.REQUIREMENTS_CHECK);
            if (module.isPersistenceRequired() && (databaseManager == null || !databaseManager.isAvailable())) {
                PersistenceState state = databaseManager != null ? databaseManager.getState() : PersistenceState.DISABLED;
                module.setLifecycleState(ModuleState.DISABLED_PERSISTENCE_UNAVAILABLE);
                module.setEnabled(false);
                logger.warning("[KUpdater] Module '" + module.getName() + "' [" + module.getId()
                        + "] disabled. Reason: Database persistence required but unavailable (State: " + state + ").");
                continue;
            }

            // 4. INITIALIZING -> ENABLED or FAILED_INITIALIZATION
            module.setLifecycleState(ModuleState.INITIALIZING);
            try {
                module.onEnable();
                module.setEnabled(true);
                module.setLifecycleState(ModuleState.ENABLED);
                enabledCount++;
                logger.info("Enabled module: " + module.getName() + " [" + module.getId() + "]");
            } catch (Throwable t) {
                module.setLifecycleState(ModuleState.FAILED_INITIALIZATION);
                module.setEnabled(false);
                logger.log(Level.SEVERE, "[KUpdater] Module '" + module.getId()
                        + "' failed to initialize cleanly. Module disabled, continuing with remaining modules.", t);
            }
        }

        logger.info("Successfully enabled " + enabledCount + "/" + modules.size() + " registered modules.");
    }


    /**
     * Disables all active registered modules.
     */
    public void disableModules() {
        for (KModule module : modules.values()) {
            if (module.isEnabled() || module.getLifecycleState() == ModuleState.ENABLED) {
                module.setLifecycleState(ModuleState.DISABLING);
                try {
                    module.onDisable();
                    module.setEnabled(false);
                    module.setLifecycleState(ModuleState.DISABLED);
                    logger.info("Disabled module: " + module.getName() + " [" + module.getId() + "]");
                } catch (Throwable t) {
                    module.setLifecycleState(ModuleState.DISABLED);
                    module.setEnabled(false);
                    logger.log(Level.SEVERE, "Error disabling module '" + module.getId() + "'", t);
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
     * Gets the capability registry for optional service discovery.
     *
     * @return capability registry
     */
    public CapabilityRegistry getCapabilityRegistry() {
        return capabilityRegistry;
    }

    /**
     * Unregisters all modules.
     */
    public void clear() {
        disableModules();
        modules.clear();
        capabilityRegistry.clear();
    }
}
