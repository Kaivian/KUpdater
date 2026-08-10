package io.github.kaivian.kupdater.platform.common.command;

import io.github.kaivian.kupdater.api.command.CommandBackend;
import io.github.kaivian.kupdater.api.command.KCommand;
import io.github.kaivian.kupdater.api.command.KCommandContext;
import io.github.kaivian.kupdater.api.module.KModule;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Fallback command backend implementation for legacy Bukkit platforms (1.8.8 to 1.20).
 * Dynamic registration via Bukkit CommandMap reflection.
 */
public class BukkitCommandBackend implements CommandBackend {

    private final Plugin plugin;
    private final Logger logger;
    private final Map<String, Command> registeredBukkitCommands = new HashMap<>();

    public BukkitCommandBackend(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger("BukkitCommandBackend");
    }

    @Override
    public boolean isSupported() {
        return true; // Supported on all Bukkit platforms
    }

    @Override
    public void registerCommands(KModule module, List<KCommand> commands) {
        if (module == null || commands == null || commands.isEmpty()) return;

        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            logger.warning("[BukkitCommandBackend] Could not obtain Bukkit CommandMap. Commands not registered.");
            return;
        }

        for (KCommand kCmd : commands) {
            Command bukkitCmd = new Command(kCmd.getName(), kCmd.getDescription(), kCmd.getUsage(), kCmd.getAliases()) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    if (!module.isEnabled()) {
                        sender.sendMessage("§cModule '" + module.getId() + "' is currently disabled.");
                        return true;
                    }

                    if (kCmd.getPermission() != null && !kCmd.getPermission().isEmpty()) {
                        if (!sender.hasPermission(kCmd.getPermission()) && !sender.isOp()) {
                            sender.sendMessage("§cYou do not have permission to execute this command.");
                            return true;
                        }
                    }

                    Map<String, Object> parsedArgs = new HashMap<>();
                    if (args.length > 0) {
                        parsedArgs.put("arg0", args[0]);
                    }
                    parsedArgs.put("rawArgs", args);

                    KCommandContext context = new KCommandContext(sender, commandLabel, parsedArgs);
                    return kCmd.execute(context);
                }

                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                    if (!module.isEnabled()) return java.util.Collections.emptyList();
                    Map<String, Object> parsedArgs = new HashMap<>();
                    parsedArgs.put("rawArgs", args);
                    KCommandContext context = new KCommandContext(sender, alias, parsedArgs);
                    return kCmd.tabComplete(context);
                }
            };

            if (kCmd.getPermission() != null) {
                bukkitCmd.setPermission(kCmd.getPermission());
            }

            commandMap.register(plugin != null ? plugin.getName().toLowerCase() : "kupdater", bukkitCmd);
            registeredBukkitCommands.put(module.getId() + ":" + kCmd.getName(), bukkitCmd);
        }
    }

    @Override
    public void unregisterCommands(KModule module) {
        if (module == null) {
            registeredBukkitCommands.clear();
            return;
        }
        registeredBukkitCommands.entrySet().removeIf(entry -> entry.getKey().startsWith(module.getId() + ":"));
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
