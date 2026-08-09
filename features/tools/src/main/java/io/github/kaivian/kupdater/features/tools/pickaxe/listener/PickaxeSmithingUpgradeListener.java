package io.github.kaivian.kupdater.features.tools.pickaxe.listener;

import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolStat;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.api.tools.service.ToolUpgradeService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

import java.util.Optional;

/**
 * Handles Smithing Table tier upgrades for Diamond Pickaxe to Netherite Pickaxe.
 */
public class PickaxeSmithingUpgradeListener implements Listener {

    private final ToolService toolService;
    private final ToolUpgradeService upgradeService;
    private final ToolConfigManager configManager;

    public PickaxeSmithingUpgradeListener(ToolService toolService, ToolUpgradeService upgradeService, ToolConfigManager configManager) {
        this.toolService = toolService;
        this.upgradeService = upgradeService;
        this.configManager = configManager;
    }

    private boolean isDiamondPickaxe(Material material) {
        return material == Material.DIAMOND_PICKAXE;
    }

    private boolean isNetheriteUpgradeTemplate(Material material) {
        if (material == null) return false;
        return "NETHERITE_UPGRADE_SMITHING_TEMPLATE".equalsIgnoreCase(material.name());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        SmithingInventory inv = event.getInventory();
        ItemStack templateItem = null;
        ItemStack pickaxeItem = null;
        ItemStack ingotItem = null;

        // In SmithingInventory, check inputs (slots before result slot)
        for (int i = 0; i < inv.getSize() - 1; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (isNetheriteUpgradeTemplate(item.getType())) {
                templateItem = item;
            } else if (isDiamondPickaxe(item.getType())) {
                pickaxeItem = item;
            } else if (item.getType() == Material.NETHERITE_INGOT) {
                ingotItem = item;
            }
        }

        if (pickaxeItem == null) return;
        if (!toolService.isManagedTool(pickaxeItem)) return;

        // Managed pickaxe requires template and netherite ingot
        if (templateItem == null || ingotItem == null) {
            event.setResult(null);
            return;
        }

        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(pickaxeItem);
        if (!progOpt.isPresent()) {
            event.setResult(null);
            return;
        }

        ToolProgression prog = progOpt.get();
        if (prog.getMaterial() != ToolMaterial.DIAMOND) {
            event.setResult(null);
            return;
        }

        int maxLvl = configManager.getMaxLevel(prog.getMaterial());
        ToolStat stat = configManager.getStat(prog.getMaterial(), prog.getLevel());

        if (prog.getLevel() < maxLvl || prog.getXp() < stat.getRequiredXp()) {
            event.setResult(null);
            return;
        }

        ToolMaterial nextTier = configManager.getNextTier(prog.getMaterial());
        if (nextTier != ToolMaterial.NETHERITE) {
            event.setResult(null);
            return;
        }

        // Build result item: Netherite Pickaxe
        ItemStack result = new ItemStack(Material.NETHERITE_PICKAXE, 1);

        int currentPoolXp = prog.getOverflowXp();
        int targetLvl = 1;
        int targetXp = currentPoolXp;
        int remainingOverflowXp = 0;

        int maxLvlNewTier = configManager.getMaxLevel(ToolMaterial.NETHERITE);
        ToolStat currentLvlStat = configManager.getStat(ToolMaterial.NETHERITE, targetLvl);

        while (targetXp >= currentLvlStat.getRequiredXp() && targetLvl < maxLvlNewTier) {
            targetXp -= currentLvlStat.getRequiredXp();
            targetLvl++;
            currentLvlStat = configManager.getStat(ToolMaterial.NETHERITE, targetLvl);
        }

        if (targetLvl >= maxLvlNewTier && targetXp > currentLvlStat.getRequiredXp()) {
            remainingOverflowXp = targetXp - currentLvlStat.getRequiredXp();
            targetXp = currentLvlStat.getRequiredXp();
        }

        ToolProgression preview = prog
                .withMaterialAndLevel(ToolMaterial.NETHERITE, targetLvl, currentLvlStat.getMaxDurability())
                .withXp(targetXp)
                .withOverflowXp(remainingOverflowXp);

        toolService.applyMetadataToItem(result, preview);

        event.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSmithingClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.SMITHING) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        SmithingInventory inv = (SmithingInventory) event.getInventory();

        ItemStack templateItem = null;
        ItemStack pickaxeItem = null;
        ItemStack ingotItem = null;

        for (int i = 0; i < inv.getSize() - 1; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (isNetheriteUpgradeTemplate(item.getType())) {
                templateItem = item;
            } else if (isDiamondPickaxe(item.getType()) && toolService.isManagedTool(item)) {
                pickaxeItem = item;
            } else if (item.getType() == Material.NETHERITE_INGOT) {
                ingotItem = item;
            }
        }

        if (pickaxeItem == null) return;
        if (templateItem == null || ingotItem == null) {
            event.setCancelled(true);
            return;
        }

        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(pickaxeItem);
        if (!progOpt.isPresent()) {
            event.setCancelled(true);
            return;
        }

        ToolProgression current = progOpt.get();
        if (current.getMaterial() != ToolMaterial.DIAMOND) {
            event.setCancelled(true);
            return;
        }

        if (!current.getOwnerUuid().equals(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This Pickaxe does not belong to you.");
            return;
        }

        int maxLvl = configManager.getMaxLevel(current.getMaterial());
        ToolStat stat = configManager.getStat(current.getMaterial(), current.getLevel());
        if (current.getLevel() < maxLvl || current.getXp() < stat.getRequiredXp()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getNotMaxLevelUpgradeSmithingMessage()));
            return;
        }

        Optional<ToolProgression> upgradedOpt = upgradeService.upgradeTier(player, pickaxeItem);
        if (upgradedOpt.isPresent()) {
            ToolProgression upgraded = upgradedOpt.get();

            ItemStack resultItem = inv.getResult();
            if (resultItem == null || resultItem.getType() == Material.AIR) {
                resultItem = new ItemStack(Material.NETHERITE_PICKAXE, 1);
            }
            toolService.applyMetadataToItem(resultItem, upgraded);
            inv.setResult(resultItem);

            String msg = configManager.getTierUpgradedMessage()
                    .replace("%tier%", upgraded.getMaterial().getDisplayName())
                    .replace("%level%", String.valueOf(upgraded.getLevel()));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }
}
