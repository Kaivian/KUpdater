package io.github.kaivian.kupdater.core.command;

import io.github.kaivian.kupdater.api.command.CommandBackend;
import io.github.kaivian.kupdater.api.command.KCommand;
import io.github.kaivian.kupdater.api.module.KModule;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Central registry and lifecycle manager for module commands in KUpdater.
 * Selects and delegates to an appropriate CommandBackend implementation.
 */
public class CommandManager {

    private final Plugin plugin;
    private final Logger logger;
    private final CommandBackend activeBackend;
    private final Map<String, List<KCommand>> moduleCommands = new LinkedHashMap<>();

    public CommandManager(Plugin plugin, CommandBackend activeBackend) {
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger("CommandManager");
        this.activeBackend = Objects.requireNonNull(activeBackend, "CommandBackend cannot be null");
        logger.info("[KUpdater] CommandManager initialized with backend: " + activeBackend.getClass().getSimpleName());
    }

    /**
     * Gets the active command backend.
     *
     * @return CommandBackend instance
     */
    public CommandBackend getActiveBackend() {
        return activeBackend;
    }

    /**
     * Registers a set of commands for an enabled module.
     *
     * @param module   owning module
     * @param commands command instances to register
     */
    public void registerModuleCommands(KModule module, KCommand... commands) {
        if (module == null || commands == null || commands.length == 0) return;
        List<KCommand> cmdList = Arrays.asList(commands);

        moduleCommands.computeIfAbsent(module.getId(), k -> new ArrayList<>()).addAll(cmdList);

        if (module.isEnabled()) {
            activeBackend.registerCommands(module, cmdList);
            logger.info("[KUpdater] Registered " + cmdList.size() + " command(s) for module '" + module.getId() + "'");
        } else {
            logger.warning("[KUpdater] Module '" + module.getId() + "' is not enabled. Commands skipped.");
        }
    }

    /**
     * Unregisters or revokes commands when a module is disabled.
     *
     * @param module owning module
     */
    public void unregisterModuleCommands(KModule module) {
        if (module == null) return;
        List<KCommand> cmds = moduleCommands.remove(module.getId());
        if (cmds != null && !cmds.isEmpty()) {
            activeBackend.unregisterCommands(module);
            logger.info("[KUpdater] Unregistered commands for module '" + module.getId() + "'");
        }
    }

    /**
     * Gets all registered commands for a specific module.
     *
     * @param moduleId unique module identifier
     * @return list of commands
     */
    public List<KCommand> getModuleCommands(String moduleId) {
        List<KCommand> cmds = moduleCommands.get(moduleId);
        return cmds != null ? Collections.unmodifiableList(cmds) : Collections.emptyList();
    }

    /**
     * Unregisters all module commands.
     */
    public void clear() {
        for (String moduleId : new ArrayList<>(moduleCommands.keySet())) {
            List<KCommand> cmds = moduleCommands.remove(moduleId);
            if (cmds != null) {
                activeBackend.unregisterCommands(null);
            }
        }
        moduleCommands.clear();
    }
}
