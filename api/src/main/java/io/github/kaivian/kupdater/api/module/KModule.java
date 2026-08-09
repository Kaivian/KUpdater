package io.github.kaivian.kupdater.api.module;

import io.github.kaivian.kupdater.api.version.MinecraftVersion;
import io.github.kaivian.kupdater.api.version.MinecraftVersionRange;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Common contract for all KUpdater gameplay enhancement modules.
 */
public interface KModule {

    /**
     * Unique identifier for the module (e.g., "tools").
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
     * Minimum Minecraft version supported by this module.
     *
     * @return minimum version
     */
    default MinecraftVersion getMinimumSupportedVersion() {
        return MinecraftVersion.V1_8_8;
    }

    /**
     * Maximum Minecraft version supported by this module.
     *
     * @return maximum version
     */
    default MinecraftVersion getMaximumSupportedVersion() {
        return MinecraftVersion.V26_2;
    }

    /**
     * Gets the supported Minecraft version range for this module.
     *
     * @return version range
     */
    default MinecraftVersionRange getSupportedVersionRange() {
        return MinecraftVersionRange.of(getMinimumSupportedVersion(), getMaximumSupportedVersion());
    }

    /**
     * Checks if this module supports the specified Minecraft version object.
     *
     * @param version MinecraftVersion instance
     * @return true if supported, false otherwise
     */
    default boolean supports(MinecraftVersion version) {
        return getSupportedVersionRange().contains(version);
    }

    /**
     * Checks if this module supports the specified Minecraft version string.
     *
     * @param versionStr Minecraft version string
     * @return true if supported, false otherwise
     */
    default boolean supports(String versionStr) {
        return getSupportedVersionRange().contains(versionStr);
    }

    /**
     * Gets the current lifecycle state of the module.
     *
     * @return lifecycle state
     */
    default ModuleState getLifecycleState() {
        return isEnabled() ? ModuleState.ENABLED : ModuleState.DISABLED;
    }

    /**
     * Sets the current lifecycle state of the module.
     *
     * @param state lifecycle state
     */
    default void setLifecycleState(ModuleState state) {
    }

    /**
     * Called when the module is enabled.
     */
    void onEnable();

    /**
     * Called when the module is disabled.
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
    }

    /**
     * Declares whether this module requires database persistence to function.
     * If true and persistence is unavailable, ModuleManager will skip enabling this module.
     *
     * @return true if database persistence is required, false otherwise
     */
    default boolean requiresPersistence() {
        return false;
    }

    /**
     * Helper check for persistence requirement.
     *
     * @return true if database persistence is required, false otherwise
     */
    default boolean isPersistenceRequired() {
        return requiresPersistence();
    }
}


