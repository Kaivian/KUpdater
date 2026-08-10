package io.github.kaivian.kupdater.bootstrap.command;

import io.github.kaivian.kupdater.bootstrap.KUpdaterPlugin;
import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.features.tools.ToolsModule;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main command executor for KUpdater (/kupdater reload).
 */
public class KUpdaterCommand implements CommandExecutor, TabCompleter {

    private final KUpdaterPlugin plugin;

    public KUpdaterCommand(KUpdaterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("tool")) {
            Optional<KModule> toolsOpt = plugin.getModuleManager().getModule("tools");
            if (!toolsOpt.isPresent() || !(toolsOpt.get() instanceof ToolsModule) || !toolsOpt.get().isEnabled()) {
                sender.sendMessage(ChatColor.RED + "Tools module is not loaded or disabled.");
                return true;
            }

            ToolsModule toolsModule = (ToolsModule) toolsOpt.get();
            if (toolsModule.getAdminService() == null) {
                sender.sendMessage(ChatColor.RED + "Tool administrative service is unavailable.");
                return true;
            }

            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, subArgs.length);

            Map<String, Object> parsedArgs = parseSubArgs(subArgs);
            io.github.kaivian.kupdater.api.command.KCommandContext context =
                    new io.github.kaivian.kupdater.api.command.KCommandContext(sender, label + " tool", parsedArgs);

            io.github.kaivian.kupdater.features.tools.command.ToolCommand toolCmd =
                    new io.github.kaivian.kupdater.features.tools.command.ToolCommand(toolsModule, toolsModule.getAdminService());
            return toolCmd.execute(context);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("recover")) {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be executed by in-game players.");
                return true;
            }
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;

            Optional<KModule> toolsOpt = plugin.getModuleManager().getModule("tools");
            if (!toolsOpt.isPresent() || !(toolsOpt.get() instanceof ToolsModule)) {
                player.sendMessage(ChatColor.RED + "Tools module is not loaded.");
                return true;
            }

            ToolsModule toolsModule = (ToolsModule) toolsOpt.get();
            io.github.kaivian.kupdater.api.tools.repository.ToolRepository repository = toolsModule.getRepository();
            io.github.kaivian.kupdater.api.tools.service.ToolService toolService = toolsModule.getToolService();

            Optional<io.github.kaivian.kupdater.api.tools.model.ToolProgression> progOpt = repository.findByOwnerAndType(player.getUniqueId(), io.github.kaivian.kupdater.api.tools.model.ToolType.PICKAXE);
            if (!progOpt.isPresent()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', toolsModule.getToolConfigManager().getRecoverNoPickaxeMessage()));
                return true;
            }

            io.github.kaivian.kupdater.api.tools.model.ToolProgression prog = progOpt.get();

            // Check if player already has a managed pickaxe in inventory
            boolean alreadyHasInInventory = false;
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                if (item != null && toolService.isManagedTool(item)) {
                    alreadyHasInInventory = true;
                    break;
                }
            }

            if (alreadyHasInInventory) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', toolsModule.getToolConfigManager().getRecoverAlreadyInInventoryMessage()));
                return true;
            }

            // Create & stamp lost item
            org.bukkit.Material bukkitMat = toolsModule.getToolConfigManager().getBukkitMaterial(prog.getMaterial());
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(bukkitMat, 1);
            toolService.applyMetadataToItem(item, prog);

            player.getInventory().addItem(item);
            String recMsg = toolsModule.getToolConfigManager().getRecoverSuccessMessage()
                    .replace("%tier%", prog.getMaterial().getDisplayName())
                    .replace("%level%", String.valueOf(prog.getLevel()));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', recMsg));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            Optional<KModule> toolsOpt = plugin.getModuleManager().getModule("tools");
            ToolConfigManager toolCfg = (toolsOpt.isPresent() && toolsOpt.get() instanceof ToolsModule)
                    ? ((ToolsModule) toolsOpt.get()).getToolConfigManager() : null;

            if (!sender.hasPermission("kupdater.admin") && !sender.isOp()) {
                String noPermMsg = toolCfg != null ? toolCfg.getNoPermissionMessage() : "&cYou do not have permission to execute this command.";
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermMsg));
                return true;
            }

            long startTime = System.currentTimeMillis();

            // 1. Reload core config
            if (plugin.getConfigManager() != null) {
                plugin.getConfigManager().reload();
            }

            // 2. Reload feature module configs
            if (plugin.getModuleManager() != null) {
                if (toolsOpt.isPresent() && toolsOpt.get() instanceof ToolsModule) {
                    ((ToolsModule) toolsOpt.get()).reloadConfig();
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            String relMsg = toolCfg != null ? toolCfg.getReloadSuccessMessage() : "&a★ [KUpdater] Configuration reloaded successfully in %duration%ms!";
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', relMsg.replace("%duration%", String.valueOf(duration))));
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "★ KUpdater Plugin v" + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sendInteractiveRootHelp(sender, "/" + label + " tool", "Tool module administrative commands", "/" + label + " tool ");
        sendInteractiveRootHelp(sender, "/" + label + " recover", "Recover your lost KUpdater Pickaxe", "/" + label + " recover");
        if (sender.hasPermission("kupdater.admin") || sender.isOp()) {
            sendInteractiveRootHelp(sender, "/" + label + " reload", "Reload plugin configuration", "/" + label + " reload");
        }
        return true;
    }

    private void sendInteractiveRootHelp(CommandSender sender, String commandText, String descriptionText, String suggestedCommand) {
        if (sender instanceof org.bukkit.entity.Player) {
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
            net.md_5.bungee.api.chat.TextComponent prefix = new net.md_5.bungee.api.chat.TextComponent(ChatColor.translateAlternateColorCodes('&', "&e - "));
            net.md_5.bungee.api.chat.TextComponent cmdComponent = new net.md_5.bungee.api.chat.TextComponent(ChatColor.translateAlternateColorCodes('&', commandText));
            net.md_5.bungee.api.chat.TextComponent descComponent = new net.md_5.bungee.api.chat.TextComponent(ChatColor.translateAlternateColorCodes('&', " &7: " + descriptionText));

            String hoverStr = "&a✦ Click to auto-fill command into chat!\n&eCommand: &f" + commandText + "\n&eDescription: &7" + descriptionText;
            net.md_5.bungee.api.chat.HoverEvent hoverEvent = new net.md_5.bungee.api.chat.HoverEvent(
                    net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', hoverStr))
            );
            net.md_5.bungee.api.chat.ClickEvent clickEvent = new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, suggestedCommand
            );

            cmdComponent.setHoverEvent(hoverEvent);
            cmdComponent.setClickEvent(clickEvent);

            prefix.addExtra(cmdComponent);
            prefix.addExtra(descComponent);
            player.spigot().sendMessage(prefix);
        } else {
            sender.sendMessage(ChatColor.YELLOW + " - " + commandText + " : " + descriptionText);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("tool")) {
            Optional<KModule> toolsOpt = plugin.getModuleManager().getModule("tools");
            if (toolsOpt.isPresent() && toolsOpt.get() instanceof ToolsModule && toolsOpt.get().isEnabled()) {
                ToolsModule toolsModule = (ToolsModule) toolsOpt.get();
                if (toolsModule.getAdminService() != null) {
                    String[] subArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, subArgs, 0, subArgs.length);

                    Map<String, Object> parsedArgs = parseSubArgs(subArgs);
                    io.github.kaivian.kupdater.api.command.KCommandContext context =
                            new io.github.kaivian.kupdater.api.command.KCommandContext(sender, alias + " tool", parsedArgs);

                    io.github.kaivian.kupdater.features.tools.command.ToolCommand toolCmd =
                            new io.github.kaivian.kupdater.features.tools.command.ToolCommand(toolsModule, toolsModule.getAdminService());
                    return toolCmd.tabComplete(context);
                }
            }
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("tool".startsWith(args[0].toLowerCase())) {
                completions.add("tool");
            }
            if ("recover".startsWith(args[0].toLowerCase())) {
                completions.add("recover");
            }
            if ((sender.hasPermission("kupdater.admin") || sender.isOp()) && "reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            return completions;
        }
        return Collections.emptyList();
    }

    private Map<String, Object> parseSubArgs(String[] subArgs) {
        Map<String, Object> parsed = new java.util.HashMap<>();
        parsed.put("rawArgs", subArgs);
        if (subArgs.length == 0) return parsed;

        parsed.put("subcommand", subArgs[0]);

        if (subArgs.length > 1) {
            parsed.put("arg1", subArgs[1]);
            try {
                int intVal = Integer.parseInt(subArgs[1]);
                parsed.put("int1", intVal);
            } catch (NumberFormatException ignored) {}

            org.bukkit.entity.Player target = plugin.getServer().getPlayer(subArgs[1]);
            if (target != null) {
                parsed.put("player", target);
                parsed.put("targetPlayer", target);
            }
        }

        if (subArgs.length > 2) {
            parsed.put("arg2", subArgs[2]);
            try {
                int intVal = Integer.parseInt(subArgs[2]);
                parsed.put("amount", intVal);
                parsed.put("level", intVal);
                parsed.put("int2", intVal);
            } catch (NumberFormatException ignored) {}

            if (!parsed.containsKey("player") && plugin.getServer() != null) {
                org.bukkit.entity.Player target = plugin.getServer().getPlayer(subArgs[2]);
                if (target != null) {
                    parsed.put("player", target);
                    parsed.put("targetPlayer", target);
                }
            }
        }

        if (subArgs.length > 3) {
            parsed.put("arg3", subArgs[3]);
            try {
                int intVal = Integer.parseInt(subArgs[3]);
                parsed.put("amount", intVal);
                parsed.put("level", intVal);
                parsed.put("int3", intVal);
            } catch (NumberFormatException ignored) {}
        }

        for (String arg : subArgs) {
            if (arg.equalsIgnoreCase("--confirm")) {
                parsed.put("confirmed", true);
            } else if (arg.equalsIgnoreCase("--replace")) {
                parsed.put("replace", true);
            }
        }

        return parsed;
    }
}
