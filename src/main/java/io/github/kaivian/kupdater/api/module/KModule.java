package io.github.kaivian.kupdater.api.module;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Common contract for all KUpdater gameplay enhancement modules.
 * <p>
 * Modules represent self-contained gameplay features that can be enabled,
 * disabled, and configured independently by server administrators.
 */
public interface KModule {

    /**
     * Unique identifier for the module (e.g., "tools-progression").
     *
     * @return unique module identifier
     */
    String getId();

    /**
     * Human-readable name for the module.
     *
     * @return display name
     */
    String getName();

    /**
     * Brief description of what the module does.
     *
     * @return module description
     */
    String getDescription();

    /**
     * Category that this module belongs to.
     *
     * @return module category
     */
    ModuleCategory getCategory();

    /**
     * Supported Minecraft versions for this module.
     *
     * @return list of supported version strings or patterns
     */
    List<String> getSupportedMinecraftVersions();

    /**
     * Called when the module is enabled.
     * Implementations should register listeners, commands, recipes, and initialize state here.
     */
    void onEnable();

    /**
     * Called when the module is disabled.
     * Implementations should unregister listeners, clean up tasks, and save state here.
     */
    void onDisable();

    /**
     * Checks whether the module is currently enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Set the enabled status of the module.
     *
     * @param enabled target status
     */
    void setEnabled(boolean enabled);

    /**
     * Reload configuration for this module.
     *
     * @param config the module-specific or core configuration
     */
    default void reloadConfig(FileConfiguration config) {
        // Optional override for custom config reloading
    }
}
