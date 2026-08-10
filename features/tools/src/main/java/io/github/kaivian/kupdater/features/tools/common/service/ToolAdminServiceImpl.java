package io.github.kaivian.kupdater.features.tools.common.service;

import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolStat;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.core.command.confirmation.CommandConfirmationService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Implementation of ToolAdminService encapsulating administrative domain rules,
 * optimistic concurrency locking retries, confirmation checks, and audit logging.
 */
public class ToolAdminServiceImpl implements ToolAdminService {

    private final ToolService toolService;
    private final ToolRepository repository;
    private final ToolConfigManager configManager;
    private final ToolItemSynchronizer itemSynchronizer;
    private final CommandConfirmationService confirmationService;
    private final Logger logger;

    public ToolAdminServiceImpl(ToolService toolService, ToolRepository repository,
                                ToolConfigManager configManager, ToolItemSynchronizer itemSynchronizer,
                                CommandConfirmationService confirmationService, Logger logger) {
        this.toolService = Objects.requireNonNull(toolService, "toolService cannot be null");
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
        this.itemSynchronizer = Objects.requireNonNull(itemSynchronizer, "itemSynchronizer cannot be null");
        this.confirmationService = Objects.requireNonNull(confirmationService, "confirmationService cannot be null");
        this.logger = logger != null ? logger : Logger.getLogger("ToolAdminService");
    }

    private void logAudit(CommandSender executor, UUID targetUuid, String action, String details) {
        String execName = executor != null ? executor.getName() : "Console";
        logger.info(String.format("[ToolAdminAudit] Executor: '%s' | Target: '%s' | Action: '%s' | %s | Timestamp: '%s'",
                execName, targetUuid, action, details, Instant.now().toString()));
    }

    @Override
    public Result addXp(UUID targetUuid, int amount, CommandSender executor) {
        if (targetUuid == null) return new Result(Status.PLAYER_NOT_FOUND, "Target player UUID cannot be null.");
        if (amount <= 0) return new Result(Status.INVALID_AMOUNT, "XP amount must be greater than 0.");

        for (int attempt = 1; attempt <= 3; attempt++) {
            Optional<ToolProgression> progOpt = repository.findByOwnerAndType(targetUuid, ToolType.PICKAXE);
            if (!progOpt.isPresent()) return new Result(Status.TOOL_NOT_FOUND, "Target player does not own a managed Pickaxe.");

            ToolProgression current = progOpt.get();
            ToolMaterial mat = current.getMaterial();
            int maxLvlInTier = configManager.getMaxLevel(mat);

            int lvl = current.getLevel();
            int xp = current.getXp() + amount;
            int overflow = current.getOverflowXp();

            ToolStat lvlStat = configManager.getStat(mat, lvl);
            int reqXp = lvlStat.getRequiredXp();

            // Handle multi-level ups
            while (xp >= reqXp && lvl < maxLvlInTier) {
                xp -= reqXp;
                lvl++;
                lvlStat = configManager.getStat(mat, lvl);
                reqXp = lvlStat.getRequiredXp();
            }

            if (lvl >= maxLvlInTier && xp >= reqXp) {
                if (configManager.isAllowOverflowXpPool()) {
                    int excess = xp - reqXp;
                    overflow += excess;
                    int maxPool = configManager.getMaxOverflowXpPool();
                    if (maxPool > 0 && overflow > maxPool) {
                        overflow = maxPool;
                    }
                }
                xp = reqXp;
            }

            ToolStat newStat = configManager.getStat(mat, lvl);
            ToolProgression updated = current.withLevel(lvl).withXp(xp).withOverflowXp(overflow)
                    .withDurability(newStat.getMaxDurability());

            ToolProgression saved = repository.save(updated);
            if (saved != null) {
                Player targetPlayer = Bukkit.getPlayer(targetUuid);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    itemSynchronizer.synchronizePlayerTool(targetPlayer, saved);
                }
                logAudit(executor, targetUuid, "XP_ADD", "Amount: +" + amount + " | New Level: " + saved.getLevel() + " | New XP: " + saved.getXp());
                return new Result(Status.SUCCESS, "Successfully added " + amount + " XP to player's Pickaxe.", saved);
            }
        }
        return new Result(Status.CONCURRENT_CONFLICT, "Concurrent modification conflict after 3 attempts. Operation aborted.");
    }

    @Override
    public Result removeXp(UUID targetUuid, int amount, CommandSender executor) {
        if (targetUuid == null) return new Result(Status.PLAYER_NOT_FOUND, "Target player UUID cannot be null.");
        if (amount <= 0) return new Result(Status.INVALID_AMOUNT, "XP amount must be greater than 0.");

        for (int attempt = 1; attempt <= 3; attempt++) {
            Optional<ToolProgression> progOpt = repository.findByOwnerAndType(targetUuid, ToolType.PICKAXE);
            if (!progOpt.isPresent()) return new Result(Status.TOOL_NOT_FOUND, "Target player does not own a managed Pickaxe.");

            ToolProgression current = progOpt.get();
            int overflow = current.getOverflowXp();
            int xp = current.getXp();

            int remainingToRemove = amount;

            if (overflow > 0) {
                if (overflow >= remainingToRemove) {
                    overflow -= remainingToRemove;
                    remainingToRemove = 0;
                } else {
                    remainingToRemove -= overflow;
                    overflow = 0;
                }
            }

            if (remainingToRemove > 0) {
                xp = Math.max(0, xp - remainingToRemove);
            }

            ToolProgression updated = current.withXp(xp).withOverflowXp(overflow);
            ToolProgression saved = repository.save(updated);
            if (saved != null) {
                Player targetPlayer = Bukkit.getPlayer(targetUuid);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    itemSynchronizer.synchronizePlayerTool(targetPlayer, saved);
                }
                logAudit(executor, targetUuid, "XP_REMOVE", "Amount: -" + amount + " | New XP: " + saved.getXp() + " | New Overflow: " + saved.getOverflowXp());
                return new Result(Status.SUCCESS, "Successfully removed " + amount + " XP from player's Pickaxe.", saved);
            }
        }
        return new Result(Status.CONCURRENT_CONFLICT, "Concurrent modification conflict after 3 attempts.");
    }

    @Override
    public Result setXp(UUID targetUuid, int amount, CommandSender executor) {
        if (targetUuid == null) return new Result(Status.PLAYER_NOT_FOUND, "Target player UUID cannot be null.");
        if (amount < 0) return new Result(Status.INVALID_AMOUNT, "XP amount cannot be negative.");

        Optional<ToolProgression> progOpt = repository.findByOwnerAndType(targetUuid, ToolType.PICKAXE);
        if (!progOpt.isPresent()) return new Result(Status.TOOL_NOT_FOUND, "Target player does not own a managed Pickaxe.");

        ToolProgression current = progOpt.get();
        ToolStat stat = configManager.getStat(current.getMaterial(), current.getLevel());
        int reqXp = stat.getRequiredXp();

        if (amount > reqXp) {
            return new Result(Status.EXCEEDS_REQUIREMENT,
                    "XP amount (" + amount + ") exceeds required XP for current level (" + reqXp + "). Use 'xp add' to trigger level-ups.");
        }

        ToolProgression updated = current.withXp(amount);
        ToolProgression saved = repository.save(updated);
        if (saved != null) {
            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                itemSynchronizer.synchronizePlayerTool(targetPlayer, saved);
            }
            logAudit(executor, targetUuid, "XP_SET", "Set XP: " + amount);
            return new Result(Status.SUCCESS, "Successfully set Pickaxe XP to " + amount + ".", saved);
        }
        return new Result(Status.ERROR, "Failed to save XP change to database.");
    }

    @Override
    public Result addLevel(UUID targetUuid, int amount, CommandSender executor) {
        if (targetUuid == null) return new Result(Status.PLAYER_NOT_FOUND, "Target player UUID cannot be null.");
        if (amount <= 0) return new Result(Status.INVALID_LEVEL, "Level amount must be greater than 0.");

        Optional<ToolProgression> progOpt = repository.findByOwnerAndType(targetUuid, ToolType.PICKAXE);
        if (!progOpt.isPresent()) return new Result(Status.TOOL_NOT_FOUND, "Target player does not own a managed Pickaxe.");

        ToolProgression current = progOpt.get();
        int maxLvl = configManager.getMaxLevel(current.getMaterial());
        int newLvl = Math.min(maxLvl, current.getLevel() + amount);

        ToolStat stat = configManager.getStat(current.getMaterial(), newLvl);
        ToolProgression updated = current.withLevel(newLvl).withDurability(stat.getMaxDurability());

        ToolProgression saved = repository.save(updated);
        if (saved != null) {
            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                itemSynchronizer.synchronizePlayerTool(targetPlayer, saved);
            }
            logAudit(executor, targetUuid, "LEVEL_ADD", "Added Levels: +" + amount + " | New Level: " + saved.getLevel());
            return new Result(Status.SUCCESS, "Successfully increased Pickaxe level to " + saved.getLevel() + ".", saved);
        }
        return new Result(Status.ERROR, "Failed to save level update to database.");
    }

    @Override
    public Result setLevel(UUID targetUuid, int targetLevel, boolean confirmed, CommandSender executor) {
        if (targetUuid == null) return new Result(Status.PLAYER_NOT_FOUND, "Target player UUID cannot be null.");

        Optional<ToolProgression> progOpt = repository.findByOwnerAndType(targetUuid, ToolType.PICKAXE);
        if (!progOpt.isPresent()) return new Result(Status.TOOL_NOT_FOUND, "Target player does not own a managed Pickaxe.");

        ToolProgression current = progOpt.get();
        int maxLvl = configManager.getMaxLevel(current.getMaterial());
        if (targetLevel < 1 || targetLevel > maxLvl) {
            return new Result(Status.INVALID_LEVEL, "Target level must be between 1 and " + maxLvl + " for tier " + current.getMaterial().getDisplayName() + ".");
        }

        if (targetLevel < current.getLevel()) {
            boolean ready = confirmationService.handleConfirmation(
                    executor,
                    confirmed,
                    "level_set_downward_" + targetUuid,
                    "Setting Pickaxe level down from " + current.getLevel() + " to " + targetLevel + " will decrease tool stats.",
                    () -> setLevel(targetUuid, targetLevel, true, executor)
            );
            if (!ready) {
                return new Result(Status.CONFIRMATION_REQUIRED, "Confirmation required to lower level.");
            }
        }

        ToolStat stat = configManager.getStat(current.getMaterial(), targetLevel);
        int clampedXp = Math.min(current.getXp(), stat.getRequiredXp());
        ToolProgression updated = current.withLevel(targetLevel).withXp(clampedXp).withDurability(stat.getMaxDurability());

        ToolProgression saved = repository.save(updated);
        if (saved != null) {
            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                itemSynchronizer.synchronizePlayerTool(targetPlayer, saved);
            }
            logAudit(executor, targetUuid, "LEVEL_SET", "Set Level: " + targetLevel);
            return new Result(Status.SUCCESS, "Successfully set Pickaxe level to " + targetLevel + ".", saved);
        }
        return new Result(Status.ERROR, "Failed to save level to database.");
    }

    @Override
    public Result resetProgression(UUID targetUuid, boolean confirmed, CommandSender executor) {
        if (targetUuid == null) return new Result(Status.PLAYER_NOT_FOUND, "Target player UUID cannot be null.");

        Optional<ToolProgression> progOpt = repository.findByOwnerAndType(targetUuid, ToolType.PICKAXE);
        if (!progOpt.isPresent()) return new Result(Status.TOOL_NOT_FOUND, "Target player does not own a managed Pickaxe.");

        boolean ready = confirmationService.handleConfirmation(
                executor,
                confirmed,
                "reset_" + targetUuid,
                "Resetting Pickaxe progression will erase all level, XP, and overflow XP state back to Wooden Level 1.",
                () -> resetProgression(targetUuid, true, executor)
        );
        if (!ready) {
            return new Result(Status.CONFIRMATION_REQUIRED, "Confirmation required to reset progression.");
        }

        ToolProgression current = progOpt.get();
        ToolStat initialStat = configManager.getStat(ToolMaterial.WOODEN, 1);
        ToolProgression resetProg = current
                .withMaterialAndLevel(ToolMaterial.WOODEN, 1, initialStat.getMaxDurability())
                .withXp(0)
                .withOverflowXp(0)
                .withState(ToolState.ACTIVE);

        ToolProgression saved = repository.save(resetProg);
        if (saved != null) {
            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                itemSynchronizer.synchronizePlayerTool(targetPlayer, saved);
            }
            logAudit(executor, targetUuid, "RESET", "Progression reset to Wooden Level 1.");
            return new Result(Status.SUCCESS, "Successfully reset Pickaxe progression for target player.", saved);
        }
        return new Result(Status.ERROR, "Failed to save reset state to database.");
    }

    @Override
    public Result repairTool(UUID targetUuid, CommandSender executor) {
        if (targetUuid == null) return new Result(Status.PLAYER_NOT_FOUND, "Target player UUID cannot be null.");

        Optional<ToolProgression> progOpt = repository.findByOwnerAndType(targetUuid, ToolType.PICKAXE);
        if (!progOpt.isPresent()) return new Result(Status.TOOL_NOT_FOUND, "Target player does not own a managed Pickaxe.");

        ToolProgression current = progOpt.get();
        ToolStat stat = configManager.getStat(current.getMaterial(), current.getLevel());
        ToolProgression repaired = current.withDurability(stat.getMaxDurability()).withState(ToolState.ACTIVE);

        ToolProgression saved = repository.save(repaired);
        if (saved != null) {
            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                itemSynchronizer.synchronizePlayerTool(targetPlayer, saved);
            }
            logAudit(executor, targetUuid, "REPAIR", "Durability restored to max (" + stat.getMaxDurability() + ")");
            return new Result(Status.SUCCESS, "Successfully repaired Pickaxe durability to max.", saved);
        }
        return new Result(Status.ERROR, "Failed to save repaired durability.");
    }

    @Override
    public Result rebuildToolItem(Player targetPlayer, CommandSender executor) {
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            return new Result(Status.PLAYER_NOT_FOUND, "Target player must be online to rebuild item metadata.");
        }

        Optional<ToolProgression> progOpt = repository.findByOwnerAndType(targetPlayer.getUniqueId(), ToolType.PICKAXE);
        if (!progOpt.isPresent()) return new Result(Status.TOOL_NOT_FOUND, "Target player does not own a managed Pickaxe.");

        ToolProgression prog = progOpt.get();
        itemSynchronizer.synchronizePlayerTool(targetPlayer, prog);
        logAudit(executor, targetPlayer.getUniqueId(), "REBUILD_ITEM", "Re-stamped physical item lore & PDC tags.");
        return new Result(Status.SUCCESS, "Successfully rebuilt Pickaxe item metadata for " + targetPlayer.getName() + ".", prog);
    }

    @Override
    public Result setToolState(UUID targetUuid, ToolState newState, boolean confirmed, CommandSender executor) {
        if (targetUuid == null) return new Result(Status.PLAYER_NOT_FOUND, "Target player UUID cannot be null.");
        if (newState == null) return new Result(Status.ERROR, "ToolState cannot be null.");

        Optional<ToolProgression> progOpt = repository.findByOwnerAndType(targetUuid, ToolType.PICKAXE);
        if (!progOpt.isPresent()) return new Result(Status.TOOL_NOT_FOUND, "Target player does not own a managed Pickaxe.");

        ToolProgression current = progOpt.get();
        if (current.getState() == newState) {
            return new Result(Status.SUCCESS, "Pickaxe state is already " + newState + ".", current);
        }

        boolean ready = confirmationService.handleConfirmation(
                executor,
                confirmed,
                "state_set_" + targetUuid,
                "Changing tool state to " + newState + " will affect tool usability.",
                () -> setToolState(targetUuid, newState, true, executor)
        );
        if (!ready) {
            return new Result(Status.CONFIRMATION_REQUIRED, "Confirmation required to change tool state.");
        }

        ToolProgression updated = current.withState(newState);
        ToolProgression saved = repository.save(updated);
        if (saved != null) {
            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                itemSynchronizer.synchronizePlayerTool(targetPlayer, saved);
            }
            logAudit(executor, targetUuid, "STATE_SET", "State changed: " + current.getState() + " -> " + newState);
            return new Result(Status.SUCCESS, "Successfully changed Pickaxe state to " + newState + ".", saved);
        }
        return new Result(Status.ERROR, "Failed to save state change to database.");
    }

    @Override
    public Result giveTool(Player targetPlayer, ToolType type, ToolMaterial material, boolean replace, boolean confirmed, CommandSender executor) {
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            return new Result(Status.PLAYER_NOT_FOUND, "Target player must be online to receive a tool.");
        }

        ToolMaterial targetMat = material != null ? material : ToolMaterial.WOODEN;
        Optional<ToolProgression> existingOpt = repository.findByOwnerAndType(targetPlayer.getUniqueId(), ToolType.PICKAXE);

        if (existingOpt.isPresent()) {
            if (!replace) {
                return new Result(Status.ALREADY_OWNS_TOOL,
                        "Target player already owns a managed Pickaxe. Use '--replace --confirm' to overwrite.");
            }

            boolean ready = confirmationService.handleConfirmation(
                    executor,
                    confirmed,
                    "give_replace_" + targetPlayer.getUniqueId(),
                    "Replacing existing Pickaxe will overwrite player's current progression record.",
                    () -> giveTool(targetPlayer, type, targetMat, true, true, executor)
            );
            if (!ready) {
                return new Result(Status.CONFIRMATION_REQUIRED, "Confirmation required to replace existing tool.");
            }
        }

        Optional<ItemStack> itemOpt = Optional.empty();
        for (ItemStack item : targetPlayer.getInventory().getContents()) {
            if (item != null && item.getType().name().endsWith("_PICKAXE") && !toolService.isManagedTool(item)) {
                itemOpt = Optional.of(item);
                break;
            }
        }

        ItemStack targetItem = itemOpt.orElseGet(() -> new ItemStack(configManager.getBukkitMaterial(targetMat)));
        Optional<ToolProgression> regOpt = toolService.registerInitialTool(targetPlayer, targetItem);

        if (regOpt.isPresent()) {
            ToolProgression prog = regOpt.get();
            if (targetItem.getType() != configManager.getBukkitMaterial(targetMat)) {
                targetItem.setType(configManager.getBukkitMaterial(targetMat));
            }
            if (targetMat != ToolMaterial.WOODEN) {
                ToolStat stat = configManager.getStat(targetMat, 1);
                prog = prog.withMaterialAndLevel(targetMat, 1, stat.getMaxDurability());
                prog = repository.save(prog);
            }
            toolService.applyMetadataToItem(targetItem, prog);
            if (!targetPlayer.getInventory().contains(targetItem)) {
                targetPlayer.getInventory().addItem(targetItem);
            }
            targetPlayer.updateInventory();

            logAudit(executor, targetPlayer.getUniqueId(), "GIVE_TOOL", "Granted " + targetMat.getDisplayName() + " Pickaxe.");
            return new Result(Status.SUCCESS, "Successfully granted " + targetMat.getDisplayName() + " Pickaxe to " + targetPlayer.getName() + ".", prog);
        }
        return new Result(Status.ERROR, "Failed to register new tool in database.");
    }

    @Override
    public Result removeTool(UUID targetUuid, boolean confirmed, CommandSender executor) {
        if (targetUuid == null) return new Result(Status.PLAYER_NOT_FOUND, "Target player UUID cannot be null.");

        Optional<ToolProgression> progOpt = repository.findByOwnerAndType(targetUuid, ToolType.PICKAXE);
        if (!progOpt.isPresent()) return new Result(Status.TOOL_NOT_FOUND, "Target player does not own a managed Pickaxe.");

        boolean ready = confirmationService.handleConfirmation(
                executor,
                confirmed,
                "remove_" + targetUuid,
                "Revoking tool will change state to LOST and clear physical item from inventory.",
                () -> removeTool(targetUuid, true, executor)
        );
        if (!ready) {
            return new Result(Status.CONFIRMATION_REQUIRED, "Confirmation required to remove tool.");
        }

        ToolProgression current = progOpt.get();
        ToolProgression updated = current.withState(ToolState.LOST);
        ToolProgression saved = repository.save(updated);

        if (saved != null) {
            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                itemSynchronizer.synchronizePlayerTool(targetPlayer, saved);
            }
            logAudit(executor, targetUuid, "REMOVE_TOOL", "Tool state set to LOST & physical item revoked.");
            return new Result(Status.SUCCESS, "Successfully revoked Pickaxe for target player.", saved);
        }
        return new Result(Status.ERROR, "Failed to save revocation state to database.");
    }
}
