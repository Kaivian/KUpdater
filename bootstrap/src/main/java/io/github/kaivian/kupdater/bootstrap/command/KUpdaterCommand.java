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
        sender.sendMessage(ChatColor.YELLOW + " - /" + label + " recover : Recover your lost KUpdater Pickaxe");
        if (sender.hasPermission("kupdater.admin") || sender.isOp()) {
            sender.sendMessage(ChatColor.YELLOW + " - /" + label + " reload : Reload plugin configuration");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
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
}
