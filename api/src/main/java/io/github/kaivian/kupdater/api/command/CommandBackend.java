package io.github.kaivian.kupdater.api.command;

import io.github.kaivian.kupdater.api.module.KModule;

import java.util.List;

/**
 * Strategy interface for registering and unregistering module command hierarchies
 * on specific Minecraft server platforms (e.g., Paper Brigadier vs legacy Bukkit).
 */
public interface CommandBackend {

    /**
     * Registers a list of commands associated with an enabled module.
     *
     * @param module   the owning feature module
     * @param commands list of commands to register
     */
    void registerCommands(KModule module, List<KCommand> commands);

    /**
     * Unregisters or revokes commands when a module is disabled or reloaded.
     *
     * @param module the owning feature module
     */
    void unregisterCommands(KModule module);

    /**
     * Checks whether this backend implementation is supported on the current runtime environment.
     *
     * @return true if supported, false otherwise
     */
    boolean isSupported();
}
