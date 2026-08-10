package io.github.kaivian.kupdater.api.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for defining executable command nodes and subcommand trees.
 */
public abstract class KCommand {

    private final String name;
    private final String description;
    private final String usage;
    private final String permission;
    private final boolean playerOnly;
    private final List<String> aliases;
    private final List<KCommand> subCommands = new ArrayList<>();

    protected KCommand(String name, String description, String usage, String permission, boolean playerOnly, List<String> aliases) {
        this.name = name;
        this.description = description != null ? description : "";
        this.usage = usage != null ? usage : "/" + name;
        this.permission = permission;
        this.playerOnly = playerOnly;
        this.aliases = aliases != null ? Collections.unmodifiableList(aliases) : Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isPlayerOnly() {
        return playerOnly;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void addSubCommand(KCommand subCommand) {
        if (subCommand != null) {
            subCommands.add(subCommand);
        }
    }

    public List<KCommand> getSubCommands() {
        return Collections.unmodifiableList(subCommands);
    }

    /**
     * Executes the command logic.
     *
     * @param context execution context containing sender and parsed arguments
     * @return true if command executed successfully
     */
    public abstract boolean execute(KCommandContext context);

    /**
     * Tab completion provider for fallback Bukkit backends.
     *
     * @param context execution context
     * @return list of suggestions
     */
    public List<String> tabComplete(KCommandContext context) {
        return Collections.emptyList();
    }
}
