package io.github.kaivian.kupdater.features.tools.pickaxe.service;

import io.github.kaivian.kupdater.api.tools.event.ToolUpgradeEvent;
import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolStat;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.api.tools.service.ToolUpgradeService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.validation.ProgressionBalanceValidator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Implementation of ToolUpgradeService for validating and executing Pickaxe level and material tier upgrades.
 */
public class PickaxeUpgradeServiceImpl implements ToolUpgradeService {

    private final ToolService toolService;
    private final ToolRepository repository;
    private final ToolConfigManager configManager;
    private final ProgressionBalanceValidator balanceValidator;
    private final Logger logger;

    public PickaxeUpgradeServiceImpl(ToolService toolService, ToolRepository repository,
                                    ToolConfigManager configManager, ProgressionBalanceValidator balanceValidator, Logger logger) {
        this.toolService = toolService;
        this.repository = repository;
        this.configManager = configManager;
        this.balanceValidator = balanceValidator;
        this.logger = logger != null ? logger : Logger.getLogger("PickaxeUpgradeService");
    }

    @Override
    public boolean canUpgrade(Player player, ItemStack itemStack) {
        if (player == null || itemStack == null) return false;
        if (!toolService.isManagedTool(itemStack)) return false;

        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(itemStack);
        if (!progOpt.isPresent()) return false;

        ToolProgression progression = progOpt.get();

        // 1. Ownership check
        if (!progression.getOwnerUuid().equals(player.getUniqueId())) return false;

        // 2. Active state check
        if (progression.getState() != ToolState.ACTIVE) return false;

        ToolMaterial currentMat = progression.getMaterial();
        int maxLvlInTier = configManager.getMaxLevel(currentMat);

        // 3. Level progression check within current tier (tier upgrades are handled via crafting)
        if (progression.getLevel() < maxLvlInTier) {
            int nextLevel = progression.getLevel() + 1;
            ToolStat nextStat = configManager.getStat(currentMat, nextLevel);
            return balanceValidator.validateStatBalance(progression.getToolType(), nextLevel, nextStat);
        } else {
            return false;
        }
    }

    @Override
    public Optional<ToolProgression> upgradeTool(Player player, ItemStack itemStack) {
        if (!canUpgrade(player, itemStack)) {
            return Optional.empty();
        }

        Optional<ToolProgression> currentOpt = toolService.getProgressionFromItem(itemStack);
        if (!currentOpt.isPresent()) return Optional.empty();

        ToolProgression current = currentOpt.get();
        ToolMaterial currentMat = current.getMaterial();
        int maxLvlInTier = configManager.getMaxLevel(currentMat);

        if (current.getLevel() >= maxLvlInTier) {
            return Optional.empty();
        }

        int nextLevel = current.getLevel() + 1;
        ToolStat nextStat = configManager.getStat(currentMat, nextLevel);

        int newDurability = nextStat.getMaxDurability();

        ToolProgression upgraded = current.withLevel(nextLevel).withDurability(newDurability);

        ToolProgression saved = repository.save(upgraded);
        toolService.applyMetadataToItem(itemStack, saved);

        logger.info("[ToolUpdater] Upgraded Pickaxe " + saved.getToolUuid() + " for player " + player.getName()
                + " to " + saved.getMaterial().getDisplayName() + " Level " + saved.getLevel());

        if (Bukkit.getServer() != null && Bukkit.getPluginManager() != null) {
            Bukkit.getPluginManager().callEvent(new ToolUpgradeEvent(player, current, saved));
        }

        return Optional.of(saved);
    }

    @Override
    public Optional<ToolProgression> upgradeTier(Player player, ItemStack itemStack) {
        if (player == null) return Optional.empty();

        Optional<ToolProgression> currentOpt = itemStack != null && toolService.isManagedTool(itemStack) ?
                toolService.getProgressionFromItem(itemStack) :
                repository.findByOwnerAndType(player.getUniqueId(), ToolType.PICKAXE);

        if (!currentOpt.isPresent()) return Optional.empty();

        ToolProgression current = currentOpt.get();

        // 1. Ownership check
        if (!current.getOwnerUuid().equals(player.getUniqueId())) return Optional.empty();

        // 2. Active state check
        if (current.getState() != ToolState.ACTIVE) return Optional.empty();

        ToolMaterial currentMat = current.getMaterial();
        int maxLvlInTier = configManager.getMaxLevel(currentMat);

        // Must be at max level and max XP of current tier
        if (current.getLevel() < maxLvlInTier) return Optional.empty();
        ToolStat maxStat = configManager.getStat(currentMat, maxLvlInTier);
        if (current.getXp() < maxStat.getRequiredXp()) return Optional.empty();

        ToolMaterial nextTier = configManager.getNextTier(currentMat);
        if (nextTier == null) return Optional.empty();

        ToolStat nextStat = configManager.getStat(nextTier, 1);
        int currentPoolXp = current.getOverflowXp();

        int targetLvl = 1;
        int targetXp = currentPoolXp;
        int remainingOverflowXp = 0;

        int maxLvlNewTier = configManager.getMaxLevel(nextTier);
        ToolStat currentLvlStat = configManager.getStat(nextTier, targetLvl);

        while (targetXp >= currentLvlStat.getRequiredXp() && targetLvl < maxLvlNewTier) {
            targetXp -= currentLvlStat.getRequiredXp();
            targetLvl++;
            currentLvlStat = configManager.getStat(nextTier, targetLvl);
        }

        if (targetLvl >= maxLvlNewTier && targetXp > currentLvlStat.getRequiredXp()) {
            remainingOverflowXp = targetXp - currentLvlStat.getRequiredXp();
            targetXp = currentLvlStat.getRequiredXp();
        }

        ToolProgression upgraded = current
                .withMaterialAndLevel(nextTier, targetLvl, currentLvlStat.getMaxDurability())
                .withXp(targetXp)
                .withOverflowXp(remainingOverflowXp);

        if (itemStack != null) {
            itemStack.setType(configManager.getBukkitMaterial(nextTier));
        }

        ToolProgression saved = repository.save(upgraded);
        if (itemStack != null) {
            toolService.applyMetadataToItem(itemStack, saved);
        }

        if (currentPoolXp > 0) {
            int usedPoolXp = currentPoolXp - remainingOverflowXp;
            player.sendMessage(ChatColor.GREEN + "★ Applied " + ChatColor.YELLOW + usedPoolXp + " XP " + ChatColor.GREEN + "from your Stored XP Pool!");
        }

        logger.info("[ToolUpdater] Upgraded Pickaxe Tier " + saved.getToolUuid() + " for player " + player.getName()
                + " to " + saved.getMaterial().getDisplayName() + " Level 1");

        if (Bukkit.getServer() != null && Bukkit.getPluginManager() != null) {
            Bukkit.getPluginManager().callEvent(new ToolUpgradeEvent(player, current, saved));
        }

        return Optional.of(saved);
    }

    @Override
    public int getMaxLevel(String toolType) {
        return configManager.getMaxLevel(ToolMaterial.WOODEN);
    }
}
