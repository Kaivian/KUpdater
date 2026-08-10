package io.github.kaivian.kupdater.platform.v1_21.command;

import io.github.kaivian.kupdater.api.command.CommandBackend;
import io.github.kaivian.kupdater.api.command.KCommand;
import io.github.kaivian.kupdater.api.command.KCommandContext;
import io.github.kaivian.kupdater.api.module.KModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Modern Paper 1.21+ / 26.x platform command backend implementation.
 * Wraps paper-native command trees, pre-parses typed arguments, and enforces module lifecycle predicates.
 */
public class PaperCommandBackend implements CommandBackend {

    private final Plugin plugin;
    private final Logger logger;
    private final Set<String> activeRegisteredModules = new HashSet<>();
    private final Map<String, Command> registeredPaperCommands = new HashMap<>();

    public PaperCommandBackend(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger("PaperCommandBackend");
    }

    @Override
    public boolean isSupported() {
        if (plugin == null || plugin.getServer() == null) return false;
        String ver = plugin.getServer().getVersion();
        return ver.contains("1.21") || ver.contains("26.") || ver.contains("Paper");
    }

    @Override
    public void registerCommands(KModule module, List<KCommand> commands) {
        if (module == null || commands == null || commands.isEmpty()) return;

        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            logger.warning("[PaperCommandBackend] Could not access CommandMap for Paper registration.");
            return;
        }

        activeRegisteredModules.add(module.getId());

        for (KCommand kCmd : commands) {
            Command paperCmd = new Command(kCmd.getName(), kCmd.getDescription(), kCmd.getUsage(), kCmd.getAliases()) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    if (!module.isEnabled() || !activeRegisteredModules.contains(module.getId())) {
                        sender.sendMessage("§cModule '" + module.getId() + "' is currently disabled.");
                        return true;
                    }

                    if (kCmd.getPermission() != null && !kCmd.getPermission().isEmpty()) {
                        if (!sender.hasPermission(kCmd.getPermission()) && !sender.isOp()) {
                            sender.sendMessage("§cYou do not have permission to execute this command.");
                            return true;
                        }
                    }

                    Map<String, Object> parsedArgs = parseTypedArguments(args);
                    KCommandContext context = new KCommandContext(sender, commandLabel, parsedArgs);
                    return kCmd.execute(context);
                }

                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                    if (!module.isEnabled() || !activeRegisteredModules.contains(module.getId())) {
                        return Collections.emptyList();
                    }
                    if (kCmd.getPermission() != null && !sender.hasPermission(kCmd.getPermission()) && !sender.isOp()) {
                        return Collections.emptyList();
                    }
                    Map<String, Object> parsedArgs = parseTypedArguments(args);
                    KCommandContext context = new KCommandContext(sender, alias, parsedArgs);
                    return kCmd.tabComplete(context);
                }
            };

            if (kCmd.getPermission() != null) {
                paperCmd.setPermission(kCmd.getPermission());
            }

            commandMap.register("kupdater", paperCmd);
            registeredPaperCommands.put(module.getId() + ":" + kCmd.getName(), paperCmd);
        }

        logger.info("[PaperCommandBackend] Successfully registered Paper command tree for module '" + module.getId() + "'");
    }

    @Override
    public void unregisterCommands(KModule module) {
        if (module == null) {
            activeRegisteredModules.clear();
            registeredPaperCommands.clear();
            return;
        }
        activeRegisteredModules.remove(module.getId());
        registeredPaperCommands.entrySet().removeIf(e -> e.getKey().startsWith(module.getId() + ":"));
        logger.info("[PaperCommandBackend] Disabled/unregistered command tree for module '" + module.getId() + "'");
    }

    private Map<String, Object> parseTypedArguments(String[] args) {
        Map<String, Object> parsed = new HashMap<>();
        parsed.put("rawArgs", args);
        if (args.length == 0) return parsed;

        parsed.put("subcommand", args[0]);

        if (args.length > 1) {
            parsed.put("arg1", args[1]);
            // Attempt numeric parsing for integer arguments
            try {
                int intVal = Integer.parseInt(args[1]);
                parsed.put("int1", intVal);
            } catch (NumberFormatException ignored) {
            }

            // Player target
            if (plugin != null && plugin.getServer() != null) {
                org.bukkit.entity.Player target = plugin.getServer().getPlayer(args[1]);
                if (target != null) {
                    parsed.put("player", target);
                    parsed.put("targetPlayer", target);
                }
            }
        }

        if (args.length > 2) {
            parsed.put("arg2", args[2]);
            try {
                int intVal = Integer.parseInt(args[2]);
                parsed.put("amount", intVal);
                parsed.put("level", intVal);
                parsed.put("int2", intVal);
            } catch (NumberFormatException ignored) {
            }

            if (!parsed.containsKey("player") && plugin != null && plugin.getServer() != null) {
                org.bukkit.entity.Player target = plugin.getServer().getPlayer(args[2]);
                if (target != null) {
                    parsed.put("player", target);
                    parsed.put("targetPlayer", target);
                }
            }
        }

        if (args.length > 3) {
            parsed.put("arg3", args[3]);
            try {
                int intVal = Integer.parseInt(args[3]);
                parsed.put("amount", intVal);
                parsed.put("level", intVal);
                parsed.put("int3", intVal);
            } catch (NumberFormatException ignored) {
            }
        }

        // Check for flags
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--confirm")) {
                parsed.put("confirmed", true);
            } else if (arg.equalsIgnoreCase("--replace")) {
                parsed.put("replace", true);
            }
        }

        return parsed;
    }

    private CommandMap getCommandMap() {
        if (plugin == null || plugin.getServer() == null) return null;
        try {
            Method getCommandMapMethod = plugin.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) getCommandMapMethod.invoke(plugin.getServer());
        } catch (Throwable t) {
            try {
                Field field = plugin.getServer().getClass().getDeclaredField("commandMap");
                field.setAccessible(true);
                return (CommandMap) field.get(plugin.getServer());
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
