package io.github.kaivian.kupdater.features.tools.command;

import io.github.kaivian.kupdater.api.command.KCommand;
import io.github.kaivian.kupdater.api.command.KCommandContext;
import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.features.tools.ToolsModule;
import io.github.kaivian.kupdater.features.tools.common.service.ToolAdminService;
import io.github.kaivian.kupdater.features.tools.common.service.ToolAdminService.Result;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Paper/Brigadier command tree definition for the Tool module (/kupdater tool ...).
 */
public class ToolCommand extends KCommand {

    private final ToolsModule toolsModule;
    private final ToolAdminService adminService;

    public ToolCommand(ToolsModule toolsModule, ToolAdminService adminService) {
        super(
                "tool",
                "Administrative command suite for Tools & Equipment module",
                "/kupdater tool <subcommand>",
                "kupdater.tool.admin",
                false,
                Arrays.asList("t")
        );
        this.toolsModule = toolsModule;
        this.adminService = adminService;
    }

    @Override
    public boolean execute(KCommandContext context) {
        Optional<String> subOpt = context.getArgument("subcommand", String.class);
        if (!subOpt.isPresent()) {
            return sendHelp(context);
        }

        String sub = subOpt.get().toLowerCase();

        switch (sub) {
            case "reload":
                return handleReload(context);
            case "info":
                return handleInfo(context);
            case "inspect":
                return handleInspect(context);
            case "xp":
                return handleXp(context);
            case "level":
                return handleLevel(context);
            case "reset":
                return handleReset(context);
            case "repair":
                return handleRepair(context);
            case "rebuild":
                return handleRebuild(context);
            case "give":
                return handleGive(context);
            case "remove":
                return handleRemove(context);
            case "state":
                return handleState(context);
            case "debug":
                return handleDebug(context);
            case "help":
            default:
                return sendHelp(context);
        }
    }

    private boolean sendHelp(KCommandContext context) {
        context.reply("&6★ &eKUpdater Tool Module Commands:");
        sendInteractiveHelpLine(context, "/kupdater tool info [player]", "View tool status & progression", "/kupdater tool info ", "kupdater.tool.info");
        sendInteractiveHelpLine(context, "/kupdater tool inspect", "Inspect held tool item metadata", "/kupdater tool inspect", "kupdater.tool.info");
        sendInteractiveHelpLine(context, "/kupdater tool xp <add|remove|set> <player> <amount>", "Manage tool XP progression", "/kupdater tool xp add ", "kupdater.tool.xp");
        sendInteractiveHelpLine(context, "/kupdater tool level <add|set> <player> <level> [--confirm]", "Manage tool level state", "/kupdater tool level set ", "kupdater.tool.level");
        sendInteractiveHelpLine(context, "/kupdater tool give <player> <type> [material] [--replace]", "Grant managed tool item", "/kupdater tool give ", "kupdater.tool.give");
        sendInteractiveHelpLine(context, "/kupdater tool remove <player> [--confirm]", "Revoke managed tool item", "/kupdater tool remove ", "kupdater.tool.remove");
        sendInteractiveHelpLine(context, "/kupdater tool repair [player]", "Restore tool durability to max", "/kupdater tool repair ", "kupdater.tool.repair");
        sendInteractiveHelpLine(context, "/kupdater tool rebuild [player]", "Re-stamp physical item PDC/lore", "/kupdater tool rebuild ", "kupdater.tool.rebuild");
        sendInteractiveHelpLine(context, "/kupdater tool state set <player> <state> [--confirm]", "Change tool lifecycle state", "/kupdater tool state set ", "kupdater.tool.state");
        sendInteractiveHelpLine(context, "/kupdater tool reset <player> [--confirm]", "Reset tool progression state", "/kupdater tool reset ", "kupdater.tool.reset");
        sendInteractiveHelpLine(context, "/kupdater tool reload", "Atomic module configuration reload", "/kupdater tool reload", "kupdater.tool.reload");
        return true;
    }

    private void sendInteractiveHelpLine(
            KCommandContext context,
            String commandText,
            String descriptionText,
            String suggestedCommand,
            String permission
    ) {
        if (context.isPlayer()) {
            Player player = context.asPlayer();

            TextComponent prefix = new TextComponent(ChatColor.translateAlternateColorCodes('&', "&e - "));
            TextComponent cmdComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', commandText));
            TextComponent descComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', " &7: " + descriptionText));

            StringBuilder hoverSb = new StringBuilder();
            hoverSb.append("&a✦ Click to auto-fill command into chat!").append("\n");
            hoverSb.append("&eCommand: &f").append(commandText).append("\n");
            hoverSb.append("&eDescription: &7").append(descriptionText);
            if (permission != null && !permission.isEmpty()) {
                hoverSb.append("\n&ePermission: &8").append(permission);
            }

            HoverEvent hoverEvent = new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', hoverSb.toString()))
            );
            ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestedCommand);

            cmdComponent.setHoverEvent(hoverEvent);
            cmdComponent.setClickEvent(clickEvent);

            prefix.addExtra(cmdComponent);
            prefix.addExtra(descComponent);

            player.spigot().sendMessage(prefix);
        } else {
            context.reply("&e - %s &7: %s", commandText, descriptionText);
        }
    }

    private boolean handleReload(KCommandContext context) {
        if (!context.sender().hasPermission("kupdater.tool.reload") && !context.sender().isOp()) {
            context.replyError("You do not have permission to execute tool reload.");
            return true;
        }

        long start = System.currentTimeMillis();
        boolean success = toolsModule.reloadConfigAtomic();
        long duration = System.currentTimeMillis() - start;

        if (success) {
            context.replySuccess("Tool module configuration reloaded successfully in %dms!", duration);
        } else {
            context.replyError("Configuration reload failed due to validation errors. Active config preserved.");
        }
        return true;
    }

    private boolean handleInfo(KCommandContext context) {
        if (!context.sender().hasPermission("kupdater.tool.info") && !context.sender().isOp()) {
            context.replyError("You do not have permission to view tool info.");
            return true;
        }

        Optional<Player> playerOpt = context.getArgument("targetPlayer", Player.class);
        Player targetPlayer = playerOpt.orElseGet(() -> context.isPlayer() ? context.asPlayer() : null);

        if (targetPlayer == null) {
            context.replyError("Usage: /kupdater tool info <player>");
            return true;
        }

        Optional<ToolProgression> progOpt = toolsModule.getRepository().findByOwnerAndType(targetPlayer.getUniqueId(), ToolType.PICKAXE);
        if (!progOpt.isPresent()) {
            context.replyError("Player %s does not own a KUpdater Pickaxe.", targetPlayer.getName());
            return true;
        }

        ToolProgression prog = progOpt.get();
        context.reply("&6★ Tool Information for %s:", targetPlayer.getName());
        context.reply("&7  Tool Type: &f%s", prog.getToolType().name());
        context.reply("&7  Material Tier: &e%s", prog.getMaterial().getDisplayName());
        context.reply("&7  Level: &a%d", prog.getLevel());
        context.reply("&7  Current XP: &a%d", prog.getXp());
        context.reply("&7  Stored Pool XP: &e%d", prog.getOverflowXp());
        context.reply("&7  Durability: &f%d", prog.getCurrentDurability());
        context.reply("&7  Lifecycle State: &b%s", prog.getState().name());
        context.reply("&7  UUID: &8%s", prog.getToolUuid().toString());
        return true;
    }

    private boolean handleInspect(KCommandContext context) {
        if (!context.isPlayer()) {
            context.replyError("Inspection command can only be run in-game.");
            return true;
        }

        Player player = context.asPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        if (held == null || held.getType() == org.bukkit.Material.AIR) {
            context.replyError("You must hold a pickaxe to inspect.");
            return true;
        }

        if (!toolsModule.getToolService().isManagedTool(held)) {
            context.replyError("The held item is not a managed KUpdater tool.");
            return true;
        }

        Optional<ToolProgression> progOpt = toolsModule.getToolService().getProgressionFromItem(held);
        if (!progOpt.isPresent()) {
            context.replyError("Could not retrieve DB progression record for held item.");
            return true;
        }

        ToolProgression prog = progOpt.get();
        context.reply("&6★ Held Item Inspection:");
        context.reply("&7  Bukkit Material: &f%s", held.getType().name());
        context.reply("&7  Owner UUID: &8%s", prog.getOwnerUuid().toString());
        context.reply("&7  Tool UUID: &8%s", prog.getToolUuid().toString());
        context.reply("&7  Material: &e%s", prog.getMaterial().name());
        context.reply("&7  Level: &a%d", prog.getLevel());
        context.reply("&7  XP: &a%d", prog.getXp());
        context.reply("&7  Durability: &f%d", prog.getCurrentDurability());
        return true;
    }

    private boolean handleXp(KCommandContext context) {
        if (!context.sender().hasPermission("kupdater.tool.xp") && !context.sender().isOp()) {
            context.replyError("You do not have permission to modify tool XP.");
            return true;
        }

        Optional<String> actionOpt = context.getArgument("arg1", String.class);
        Optional<Player> targetOpt = context.getArgument("targetPlayer", Player.class);
        Optional<Integer> amtOpt = context.getArgument("amount", Integer.class);

        if (!actionOpt.isPresent() || !targetOpt.isPresent() || !amtOpt.isPresent()) {
            context.replyError("Usage: /kupdater tool xp <add|remove|set> <player> <amount>");
            return true;
        }

        String action = actionOpt.get().toLowerCase();
        Player target = targetOpt.get();
        int amount = amtOpt.get();

        Result res;
        switch (action) {
            case "add":
                res = adminService.addXp(target.getUniqueId(), amount, context.sender());
                break;
            case "remove":
                res = adminService.removeXp(target.getUniqueId(), amount, context.sender());
                break;
            case "set":
                res = adminService.setXp(target.getUniqueId(), amount, context.sender());
                break;
            default:
                context.replyError("Unknown XP action: %s. Use add, remove, or set.", action);
                return true;
        }

        if (res.isSuccess()) {
            context.replySuccess(res.getMessage());
        } else {
            context.replyError(res.getMessage());
        }
        return true;
    }

    private boolean handleLevel(KCommandContext context) {
        if (!context.sender().hasPermission("kupdater.tool.level") && !context.sender().isOp()) {
            context.replyError("You do not have permission to modify tool levels.");
            return true;
        }

        Optional<String> actionOpt = context.getArgument("arg1", String.class);
        Optional<Player> targetOpt = context.getArgument("targetPlayer", Player.class);
        Optional<Integer> lvlOpt = context.getArgument("level", Integer.class);
        boolean confirmed = context.getArgument("confirmed", Boolean.class).orElse(false);

        if (!actionOpt.isPresent() || !targetOpt.isPresent() || !lvlOpt.isPresent()) {
            context.replyError("Usage: /kupdater tool level <add|set> <player> <level> [--confirm]");
            return true;
        }

        String action = actionOpt.get().toLowerCase();
        Player target = targetOpt.get();
        int val = lvlOpt.get();

        Result res;
        if (action.equalsIgnoreCase("add")) {
            res = adminService.addLevel(target.getUniqueId(), val, context.sender());
        } else if (action.equalsIgnoreCase("set")) {
            res = adminService.setLevel(target.getUniqueId(), val, confirmed, context.sender());
        } else {
            context.replyError("Unknown level action: %s. Use add or set.", action);
            return true;
        }

        if (res.isSuccess()) {
            context.replySuccess(res.getMessage());
        } else if (res.getStatus() == ToolAdminService.Status.CONFIRMATION_REQUIRED) {
            // Confirmation prompt already sent to sender
        } else {
            context.replyError(res.getMessage());
        }
        return true;
    }

    private boolean handleReset(KCommandContext context) {
        if (!context.sender().hasPermission("kupdater.tool.reset") && !context.sender().isOp()) {
            context.replyError("You do not have permission to reset progression.");
            return true;
        }

        Optional<Player> targetOpt = context.getArgument("targetPlayer", Player.class);
        if (!targetOpt.isPresent()) {
            context.replyError("Usage: /kupdater tool reset <player> [--confirm]");
            return true;
        }

        boolean confirmed = context.getArgument("confirmed", Boolean.class).orElse(false);
        Result res = adminService.resetProgression(targetOpt.get().getUniqueId(), confirmed, context.sender());

        if (res.isSuccess()) {
            context.replySuccess(res.getMessage());
        } else if (res.getStatus() != ToolAdminService.Status.CONFIRMATION_REQUIRED) {
            context.replyError(res.getMessage());
        }
        return true;
    }

    private boolean handleRepair(KCommandContext context) {
        if (!context.sender().hasPermission("kupdater.tool.repair") && !context.sender().isOp()) {
            context.replyError("You do not have permission to repair tools.");
            return true;
        }

        Optional<Player> targetOpt = context.getArgument("targetPlayer", Player.class);
        Player target = targetOpt.orElseGet(() -> context.isPlayer() ? context.asPlayer() : null);

        if (target == null) {
            context.replyError("Usage: /kupdater tool repair <player>");
            return true;
        }

        Result res = adminService.repairTool(target.getUniqueId(), context.sender());
        if (res.isSuccess()) {
            context.replySuccess(res.getMessage());
        } else {
            context.replyError(res.getMessage());
        }
        return true;
    }

    private boolean handleRebuild(KCommandContext context) {
        if (!context.sender().hasPermission("kupdater.tool.rebuild") && !context.sender().isOp()) {
            context.replyError("You do not have permission to rebuild tool items.");
            return true;
        }

        Optional<Player> targetOpt = context.getArgument("targetPlayer", Player.class);
        Player target = targetOpt.orElseGet(() -> context.isPlayer() ? context.asPlayer() : null);

        if (target == null) {
            context.replyError("Usage: /kupdater tool rebuild <player>");
            return true;
        }

        Result res = adminService.rebuildToolItem(target, context.sender());
        if (res.isSuccess()) {
            context.replySuccess(res.getMessage());
        } else {
            context.replyError(res.getMessage());
        }
        return true;
    }

    private boolean handleGive(KCommandContext context) {
        if (!context.sender().hasPermission("kupdater.tool.give") && !context.sender().isOp()) {
            context.replyError("You do not have permission to give tools.");
            return true;
        }

        Optional<Player> targetOpt = context.getArgument("targetPlayer", Player.class);
        if (!targetOpt.isPresent()) {
            context.replyError("Usage: /kupdater tool give <player> [material] [--replace] [--confirm]");
            return true;
        }

        String rawMat = null;
        Optional<String> arg2Opt = context.getArgument("arg2", String.class);
        Optional<String> arg3Opt = context.getArgument("arg3", String.class);
        if (arg2Opt.isPresent()) {
            if (arg2Opt.get().equalsIgnoreCase("PICKAXE") && arg3Opt.isPresent()) {
                rawMat = arg3Opt.get();
            } else if (!arg2Opt.get().startsWith("--")) {
                rawMat = arg2Opt.get();
            }
        }
        ToolMaterial mat = rawMat != null ? ToolMaterial.fromString(rawMat) : ToolMaterial.WOODEN;
        boolean replace = context.getArgument("replace", Boolean.class).orElse(false);
        boolean confirmed = context.getArgument("confirmed", Boolean.class).orElse(false);

        Result res = adminService.giveTool(targetOpt.get(), ToolType.PICKAXE, mat, replace, confirmed, context.sender());
        if (res.isSuccess()) {
            context.replySuccess(res.getMessage());
        } else if (res.getStatus() != ToolAdminService.Status.CONFIRMATION_REQUIRED) {
            context.replyError(res.getMessage());
        }
        return true;
    }

    private boolean handleRemove(KCommandContext context) {
        if (!context.sender().hasPermission("kupdater.tool.remove") && !context.sender().isOp()) {
            context.replyError("You do not have permission to remove tools.");
            return true;
        }

        Optional<Player> targetOpt = context.getArgument("targetPlayer", Player.class);
        if (!targetOpt.isPresent()) {
            context.replyError("Usage: /kupdater tool remove <player> [--confirm]");
            return true;
        }

        boolean confirmed = context.getArgument("confirmed", Boolean.class).orElse(false);
        Result res = adminService.removeTool(targetOpt.get().getUniqueId(), confirmed, context.sender());

        if (res.isSuccess()) {
            context.replySuccess(res.getMessage());
        } else if (res.getStatus() != ToolAdminService.Status.CONFIRMATION_REQUIRED) {
            context.replyError(res.getMessage());
        }
        return true;
    }

    private boolean handleState(KCommandContext context) {
        if (!context.sender().hasPermission("kupdater.tool.state") && !context.sender().isOp()) {
            context.replyError("You do not have permission to set tool state.");
            return true;
        }

        Optional<Player> targetOpt = context.getArgument("targetPlayer", Player.class);
        Optional<String> stateStrOpt = context.getArgument("arg3", String.class);

        if (!targetOpt.isPresent() || !stateStrOpt.isPresent()) {
            context.replyError("Usage: /kupdater tool state set <player> <ACTIVE|LOST|DESTROYED> [--confirm]");
            return true;
        }

        ToolState state;
        try {
            state = ToolState.valueOf(stateStrOpt.get().toUpperCase());
        } catch (IllegalArgumentException e) {
            context.replyError("Invalid state: %s. Valid states: ACTIVE, LOST, DESTROYED.", stateStrOpt.get());
            return true;
        }

        boolean confirmed = context.getArgument("confirmed", Boolean.class).orElse(false);
        Result res = adminService.setToolState(targetOpt.get().getUniqueId(), state, confirmed, context.sender());
        if (res.isSuccess()) {
            context.replySuccess(res.getMessage());
        } else if (res.getStatus() != ToolAdminService.Status.CONFIRMATION_REQUIRED) {
            context.replyError(res.getMessage());
        }
        return true;
    }

    private boolean handleDebug(KCommandContext context) {
        if (!context.sender().hasPermission("kupdater.tool.debug") && !context.sender().isOp()) {
            context.replyError("You do not have permission to execute debug commands.");
            return true;
        }

        Optional<String> actionOpt = context.getArgument("arg1", String.class);
        Optional<Player> targetOpt = context.getArgument("targetPlayer", Player.class);

        if (!actionOpt.isPresent() || !targetOpt.isPresent()) {
            context.replyError("Usage: /kupdater tool debug <dump|validate> <player>");
            return true;
        }

        String action = actionOpt.get().toLowerCase();
        Player target = targetOpt.get();

        Optional<ToolProgression> progOpt = toolsModule.getRepository().findByOwnerAndType(target.getUniqueId(), ToolType.PICKAXE);
        if (!progOpt.isPresent()) {
            context.replyError("No database record found for %s.", target.getName());
            return true;
        }

        ToolProgression prog = progOpt.get();

        if (action.equalsIgnoreCase("dump")) {
            context.reply("&6★ Debug Diagnostic Dump for %s:", target.getName());
            context.reply("&7  JSON: %s", prog.toString());
            return true;
        } else if (action.equalsIgnoreCase("validate")) {
            context.reply("&6★ Validation Audit for %s:", target.getName());
            context.reply("&a✓ Database progression record exists.");
            context.reply("&a✓ Schema Version: %d", prog.getSchemaVersion());
            context.reply("&a✓ Durability: %d", prog.getCurrentDurability());
            return true;
        }

        context.replyError("Unknown debug action: %s. Use dump or validate.", action);
        return true;
    }

    @Override
    public List<String> tabComplete(KCommandContext context) {
        String[] args = (String[]) context.getParsedArguments().get("rawArgs");
        if (args == null || args.length == 0) {
            return Arrays.asList("info", "inspect", "xp", "level", "give", "remove", "repair", "rebuild", "state", "reset", "reload", "help", "debug");
        }

        if (args.length == 1) {
            return filterPrefix(Arrays.asList(
                    "info", "inspect", "xp", "level", "give", "remove", "repair", "rebuild", "state", "reset", "reload", "help", "debug"
            ), args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "xp":
                    return filterPrefix(Arrays.asList("add", "remove", "set"), args[1]);
                case "level":
                    return filterPrefix(Arrays.asList("add", "set"), args[1]);
                case "debug":
                    return filterPrefix(Arrays.asList("dump", "validate"), args[1]);
                case "state":
                    return filterPrefix(Collections.singletonList("set"), args[1]);
                case "info":
                case "give":
                case "remove":
                case "repair":
                case "rebuild":
                case "reset":
                    return getPlayerSuggestions(args[1]);
                default:
                    return Collections.emptyList();
            }
        }

        if (args.length == 3) {
            switch (sub) {
                case "xp":
                case "level":
                case "state":
                case "debug":
                    return getPlayerSuggestions(args[2]);
                case "give":
                    return filterPrefix(getConfiguredMaterialsAndTypes(), args[2]);
                case "remove":
                case "reset":
                case "repair":
                case "rebuild":
                    return suggestFlags(args[2], args, "--confirm");
                default:
                    return Collections.emptyList();
            }
        }

        if (args.length == 4) {
            switch (sub) {
                case "xp":
                    return filterPrefix(Arrays.asList("10", "50", "100", "500", "1000"), args[3]);
                case "level":
                    return filterPrefix(getConfiguredLevelSuggestions(), args[3]);
                case "state":
                    return filterPrefix(Arrays.asList("ACTIVE", "LOST", "DESTROYED"), args[3]);
                case "give":
                    if (args[2].equalsIgnoreCase("PICKAXE")) {
                        return filterPrefix(getConfiguredMaterialsOnly(), args[3]);
                    } else {
                        return suggestFlags(args[3], args, "--replace", "--confirm");
                    }
                case "remove":
                case "reset":
                case "repair":
                case "rebuild":
                    return suggestFlags(args[3], args, "--confirm");
                default:
                    return Collections.emptyList();
            }
        }

        if (args.length >= 5) {
            String current = args[args.length - 1];
            switch (sub) {
                case "level":
                case "state":
                case "remove":
                case "reset":
                case "repair":
                case "rebuild":
                    return suggestFlags(current, args, "--confirm");
                case "give":
                    return suggestFlags(current, args, "--replace", "--confirm");
                default:
                    return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }

    private List<String> getConfiguredMaterialsAndTypes() {
        List<String> suggestions = new ArrayList<>();
        if (toolsModule != null && toolsModule.getToolConfigManager() != null) {
            for (ToolMaterial mat : toolsModule.getToolConfigManager().getConfiguredMaterials()) {
                suggestions.add(mat.name());
            }
        }
        if (suggestions.isEmpty()) {
            for (ToolMaterial mat : ToolMaterial.values()) {
                suggestions.add(mat.name());
            }
        }
        for (ToolType type : ToolType.values()) {
            if (!suggestions.contains(type.name())) {
                suggestions.add(type.name());
            }
        }
        return suggestions;
    }

    private List<String> getConfiguredMaterialsOnly() {
        List<String> suggestions = new ArrayList<>();
        if (toolsModule != null && toolsModule.getToolConfigManager() != null) {
            for (ToolMaterial mat : toolsModule.getToolConfigManager().getConfiguredMaterials()) {
                suggestions.add(mat.name());
            }
        }
        if (suggestions.isEmpty()) {
            for (ToolMaterial mat : ToolMaterial.values()) {
                suggestions.add(mat.name());
            }
        }
        return suggestions;
    }

    private List<String> getConfiguredLevelSuggestions() {
        int maxLevel = 3;
        if (toolsModule != null && toolsModule.getToolConfigManager() != null) {
            for (ToolMaterial mat : toolsModule.getToolConfigManager().getConfiguredMaterials()) {
                int cfgMax = toolsModule.getToolConfigManager().getMaxLevel(mat);
                if (cfgMax > maxLevel) {
                    maxLevel = cfgMax;
                }
            }
        }
        List<String> levels = new ArrayList<>();
        for (int i = 1; i <= maxLevel; i++) {
            levels.add(String.valueOf(i));
        }
        return levels;
    }

    private List<String> suggestFlags(String currentArg, String[] allArgs, String... allowedFlags) {
        List<String> suggestions = new ArrayList<>();
        String prefix = currentArg.toLowerCase();
        for (String flag : allowedFlags) {
            if (!flagAlreadyPresent(flag, allArgs) && flag.toLowerCase().startsWith(prefix)) {
                suggestions.add(flag);
            }
        }
        return suggestions;
    }

    private boolean flagAlreadyPresent(String flag, String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(flag)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getPlayerSuggestions(String prefix) {
        List<String> playerNames = new ArrayList<>();
        String p = prefix.toLowerCase();
        if (Bukkit.getServer() != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(p)) {
                    playerNames.add(player.getName());
                }
            }
        }
        return playerNames;
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        String p = prefix.toLowerCase();
        for (String item : list) {
            if (item.toLowerCase().startsWith(p)) {
                result.add(item);
            }
        }
        return result;
    }
}
