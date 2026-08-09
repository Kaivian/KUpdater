package io.github.kaivian.kupdater.features.tools.pickaxe.listener;

import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolStat;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolOwnershipService;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.api.tools.service.ToolUpgradeService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager.UpgradeRecipe;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Grants block-specific XP to Pickaxes upon breaking blocks and triggers level/tier upgrades.
 */
public class PickaxeBlockXpListener implements Listener {

    private final ToolService toolService;
    private final ToolOwnershipService ownershipService;
    private final ToolUpgradeService upgradeService;
    private final ToolRepository repository;
    private final ToolConfigManager configManager;

    public PickaxeBlockXpListener(ToolService toolService, ToolOwnershipService ownershipService,
                                  ToolUpgradeService upgradeService, ToolRepository repository,
                                  ToolConfigManager configManager) {
        this.toolService = toolService;
        this.ownershipService = ownershipService;
        this.upgradeService = upgradeService;
        this.repository = repository;
        this.configManager = configManager;
    }

    private boolean isPickaxe(Material material) {
        if (material == null) return false;
        return material.name().endsWith("_PICKAXE");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) return;
        if (!isPickaxe(item.getType())) return;

        if (!toolService.isManagedTool(item)) {
            Optional<ToolProgression> regOpt = toolService.registerInitialTool(player, item);
            if (!regOpt.isPresent()) return;
        }

        if (!ownershipService.isOwner(player, item)) return;

        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(item);
        if (!progOpt.isPresent()) return;

        ToolProgression progression = progOpt.get();
        if (progression.getState() != ToolState.ACTIVE) return;

        Material blockType = event.getBlock().getType();
        int xpGained = configManager.getBlockXp(blockType);
        if (xpGained <= 0) return;

        ToolStat currentStat = configManager.getStat(progression.getMaterial(), progression.getLevel());
        int requiredXp = currentStat.getRequiredXp();
        int newXp = progression.getXp() + xpGained;

        if (newXp >= requiredXp) {
            int excessXp = newXp - requiredXp;
            Optional<ToolProgression> upgradedOpt = upgradeService.upgradeTool(player, item);

            if (upgradedOpt.isPresent()) {
                ToolProgression upgraded = upgradedOpt.get().withXp(excessXp);
                repository.save(upgraded);
                toolService.applyMetadataToItem(item, upgraded);
                player.getInventory().setItemInMainHand(item);
                player.updateInventory();

                String msg = configManager.getLevelUpgradedMessage()
                        .replace("%tier%", upgraded.getMaterial().getDisplayName())
                        .replace("%level%", String.valueOf(upgraded.getLevel()));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            } else {
                // Max level of current tier reached - cap XP at requiredXp and accumulate overflow XP into pool if enabled
                boolean justHitMax = progression.getXp() < requiredXp;

                int newOverflowXp = progression.getOverflowXp();
                if (configManager != null && configManager.isAllowOverflowXpPool()) {
                    int addAmount = (progression.getXp() >= requiredXp) ? xpGained : excessXp;
                    newOverflowXp += addAmount;
                    int maxPool = configManager.getMaxOverflowXpPool();
                    if (maxPool > 0 && newOverflowXp > maxPool) {
                        newOverflowXp = maxPool;
                    }
                }
                ToolProgression capped = progression.withXp(requiredXp).withOverflowXp(newOverflowXp);
                repository.save(capped);
                toolService.applyMetadataToItem(item, capped);
                player.getInventory().setItemInMainHand(item);
                player.updateInventory();

                if (justHitMax) {
                    sendMaxLevelChatMessage(player, capped);
                }
            }
        } else {
            ToolProgression updated = progression.withXp(newXp);
            repository.save(updated);
            toolService.applyMetadataToItem(item, updated);
            player.getInventory().setItemInMainHand(item);
            player.updateInventory();
        }
    }

    private void sendMaxLevelChatMessage(Player player, ToolProgression progression) {
        if (configManager == null || player == null) return;
        ToolMaterial mat = progression.getMaterial();
        ToolMaterial nextTier = configManager.getNextTier(mat);
        if (nextTier == null) return;

        if (mat == ToolMaterial.DIAMOND || nextTier == ToolMaterial.NETHERITE) {
            String tmpl = configManager.getMaxLevelGuideSmithing();
            String msg = tmpl
                    .replace("%upgrade_amount%", "1")
                    .replace("%upgrade_item%", "Netherite Ingot")
                    .replace("%upgrade_target%", nextTier.getDisplayName());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        } else {
            UpgradeRecipe recipe = configManager.getUpgradeRecipe(mat);
            if (recipe != null) {
                String tmpl = configManager.getMaxLevelGuideCrafting();
                String msg = tmpl
                        .replace("%upgrade_amount%", String.valueOf(recipe.getAmount()))
                        .replace("%upgrade_item%", formatItemName(recipe.getMaterial()))
                        .replace("%upgrade_target%", nextTier.getDisplayName());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }
        }
    }

    private String formatItemName(Material mat) {
        if (mat == null) return "";
        String[] parts = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return sb.toString();
    }
}
