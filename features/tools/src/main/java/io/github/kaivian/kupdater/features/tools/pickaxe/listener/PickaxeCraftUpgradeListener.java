package io.github.kaivian.kupdater.features.tools.pickaxe.listener;

import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolStat;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.api.tools.service.ToolUpgradeService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Handles Crafting Table tier upgrades for Pickaxes at max level of their current tier.
 */
public class PickaxeCraftUpgradeListener implements Listener {

    private final ToolService toolService;
    private final ToolUpgradeService upgradeService;
    private final ToolRepository repository;
    private final ToolConfigManager configManager;

    private boolean isKUpdaterUpgradeRecipe(org.bukkit.inventory.Recipe recipe) {
        if (recipe instanceof org.bukkit.Keyed) {
            org.bukkit.Keyed keyed = (org.bukkit.Keyed) recipe;
            return keyed.getKey().getNamespace().equalsIgnoreCase(plugin.getName());
        }
        return false;
    }

    private final org.bukkit.plugin.Plugin plugin;

    public PickaxeCraftUpgradeListener(ToolService toolService, ToolUpgradeService upgradeService, ToolRepository repository, ToolConfigManager configManager) {
        this(toolService, upgradeService, repository, configManager, org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(PickaxeCraftUpgradeListener.class));
    }

    public PickaxeCraftUpgradeListener(ToolService toolService, ToolUpgradeService upgradeService, ToolRepository repository, ToolConfigManager configManager, org.bukkit.plugin.Plugin plugin) {
        this.toolService = toolService;
        this.upgradeService = upgradeService;
        this.repository = repository;
        this.configManager = configManager;
        this.plugin = plugin;
    }

    private boolean isPickaxe(Material material) {
        if (material == null) return false;
        return material.name().endsWith("_PICKAXE");
    }

    private boolean isUpgradeMaterialMatch(Material required, Material inSlot) {
        if (required == null || inSlot == null) return false;
        if (required == inSlot) return true;

        if ((required == Material.STONE || required == Material.COBBLESTONE) &&
            (inSlot == Material.STONE || inSlot == Material.COBBLESTONE || inSlot == Material.MOSSY_COBBLESTONE)) {
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        if (matrix == null || matrix.length == 0) return;

        // Case 1: Wooden Pickaxe Crafting
        if (event.getRecipe() != null && event.getRecipe().getResult() != null
                && event.getRecipe().getResult().getType() == Material.WOODEN_PICKAXE
                && !isKUpdaterUpgradeRecipe(event.getRecipe())) {

            if (!(event.getView().getPlayer() instanceof Player)) return;
            Player player = (Player) event.getView().getPlayer();

            Optional<ToolProgression> progOpt = repository.findByOwnerAndType(player.getUniqueId(), ToolType.PICKAXE);
            if (progOpt.isPresent()) {
                // Player already owns a pickaxe -> block second wooden pickaxe craft preview!
                inv.setResult(new ItemStack(Material.AIR));
                return;
            }

            // Player does NOT own a pickaxe -> format preview result as KUpdater Wooden Pickaxe Level 1!
            ToolStat lvl1Stat = configManager.getStat(ToolMaterial.WOODEN, 1);
            int maxDurability = lvl1Stat != null ? lvl1Stat.getMaxDurability() : 59;

            ToolProgression previewProg = new ToolProgression(
                    java.util.UUID.randomUUID(),
                    player.getUniqueId(),
                    ToolType.PICKAXE,
                    ToolMaterial.WOODEN,
                    1, 0, 0,
                    io.github.kaivian.kupdater.api.tools.model.ToolState.ACTIVE,
                    maxDurability, 1,
                    java.time.Instant.now(), java.time.Instant.now()
            );

            ItemStack resultItem = new ItemStack(Material.WOODEN_PICKAXE, 1);
            toolService.applyMetadataToItem(resultItem, previewProg);
            inv.setResult(resultItem);
            return;
        }

        // Case 3: Block vanilla crafting of pickaxes above Wooden tier (e.g. Stone, Iron, Gold, Diamond from ingots + sticks)
        if (event.getRecipe() != null && event.getRecipe().getResult() != null) {
            String resultTypeName = event.getRecipe().getResult().getType().name();
            if (resultTypeName.endsWith("_PICKAXE")
                    && !resultTypeName.equalsIgnoreCase("WOODEN_PICKAXE")
                    && !isKUpdaterUpgradeRecipe(event.getRecipe())) {
                inv.setResult(new ItemStack(Material.AIR));
                return;
            }
        }

        // Case 2: Tier Upgrades
        ItemStack pickaxeItem = null;
        int pickaxeCount = 0;
        Material otherMaterial = null;
        int totalOtherCount = 0;

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (isPickaxe(item.getType())) {
                pickaxeItem = item;
                pickaxeCount++;
            } else {
                if (otherMaterial == null) {
                    otherMaterial = item.getType();
                } else if (otherMaterial != item.getType()) {
                    return;
                }
                totalOtherCount += item.getAmount();
            }
        }

        if (pickaxeCount != 1 || otherMaterial == null || pickaxeItem == null) return;
        if (!toolService.isManagedTool(pickaxeItem)) return;

        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(pickaxeItem);
        if (!progOpt.isPresent()) return;

        ToolProgression prog = progOpt.get();
        int maxLvl = configManager.getMaxLevel(prog.getMaterial());
        ToolStat stat = configManager.getStat(prog.getMaterial(), prog.getLevel());

        ToolMaterial nextTier = configManager.getNextTier(prog.getMaterial());
        if (nextTier == null) return;

        ToolConfigManager.UpgradeRecipe recipe = configManager.getUpgradeRecipe(prog.getMaterial());
        if (recipe == null) return;

        if (!isUpgradeMaterialMatch(recipe.getMaterial(), otherMaterial) || totalOtherCount < recipe.getAmount()) {
            return;
        }

        if (prog.getLevel() < maxLvl || prog.getXp() < stat.getRequiredXp()) {
            inv.setResult(new ItemStack(Material.AIR));
            return;
        }

        Material targetMat = configManager.getBukkitMaterial(nextTier);
        ItemStack result = new ItemStack(targetMat, 1);

        int currentPoolXp = prog.getOverflowXp();
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

        ToolProgression preview = prog
                .withMaterialAndLevel(nextTier, targetLvl, currentLvlStat.getMaxDurability())
                .withXp(targetXp)
                .withOverflowXp(remainingOverflowXp);

        toolService.applyMetadataToItem(result, preview);

        inv.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        if (matrix == null) return;

        // Case 1: Wooden Pickaxe Crafting
        if (event.getRecipe() != null && event.getRecipe().getResult() != null
                && event.getRecipe().getResult().getType() == Material.WOODEN_PICKAXE
                && !isKUpdaterUpgradeRecipe(event.getRecipe())) {

            Optional<ToolProgression> progOpt = repository.findByOwnerAndType(player.getUniqueId(), ToolType.PICKAXE);
            if (progOpt.isPresent()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getAlreadyOwnPickaxeMessage()));
                return;
            }

            // First Wooden Pickaxe craft: Create new progression & save to DB!
            ToolStat lvl1Stat = configManager.getStat(ToolMaterial.WOODEN, 1);
            int maxDurability = lvl1Stat != null ? lvl1Stat.getMaxDurability() : 59;

            ToolProgression newProg = new ToolProgression(
                    java.util.UUID.randomUUID(),
                    player.getUniqueId(),
                    ToolType.PICKAXE,
                    ToolMaterial.WOODEN,
                    1, 0, 0,
                    io.github.kaivian.kupdater.api.tools.model.ToolState.ACTIVE,
                    maxDurability, 1,
                    java.time.Instant.now(), java.time.Instant.now()
            );

            repository.save(newProg);

            ItemStack resultItem = event.getCurrentItem();
            if (resultItem == null || resultItem.getType() == Material.AIR) {
                resultItem = new ItemStack(Material.WOODEN_PICKAXE, 1);
            }
            toolService.applyMetadataToItem(resultItem, newProg);
            event.setCurrentItem(resultItem);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getFirstPickaxeCraftedMessage()));
            return;
        }

        // Case 3: Block vanilla crafting of pickaxes above Wooden tier
        if (event.getRecipe() != null && event.getRecipe().getResult() != null) {
            String resultTypeName = event.getRecipe().getResult().getType().name();
            if (resultTypeName.endsWith("_PICKAXE")
                    && !resultTypeName.equalsIgnoreCase("WOODEN_PICKAXE")
                    && !isKUpdaterUpgradeRecipe(event.getRecipe())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getVanillaCraftingDisabledMessage()));
                return;
            }
        }

        // Case 2: Tier Upgrades
        ItemStack pickaxeItem = null;
        for (ItemStack item : matrix) {
            if (item != null && isPickaxe(item.getType()) && toolService.isManagedTool(item)) {
                pickaxeItem = item;
                break;
            }
        }

        if (pickaxeItem == null) return;

        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(pickaxeItem);
        if (!progOpt.isPresent()) return;

        ToolProgression current = progOpt.get();
        if (!current.getOwnerUuid().equals(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getOwnershipDenyMessage()));
            return;
        }

        int maxLvl = configManager.getMaxLevel(current.getMaterial());
        ToolStat stat = configManager.getStat(current.getMaterial(), current.getLevel());
        if (current.getLevel() < maxLvl || current.getXp() < stat.getRequiredXp()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getNotMaxLevelUpgradeMessage()));
            return;
        }

        ToolMaterial nextTier = configManager.getNextTier(current.getMaterial());
        if (nextTier == null) return;

        Optional<ToolProgression> upgradedOpt = upgradeService.upgradeTier(player, pickaxeItem);
        if (upgradedOpt.isPresent()) {
            ToolProgression upgraded = upgradedOpt.get();

            Material targetMat = configManager.getBukkitMaterial(upgraded.getMaterial());
            ItemStack resultItem = event.getCurrentItem();
            if (resultItem == null || resultItem.getType() == Material.AIR || resultItem.getType() != targetMat) {
                resultItem = new ItemStack(targetMat, 1);
            }
            toolService.applyMetadataToItem(resultItem, upgraded);
            event.setCurrentItem(resultItem);

            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.getCursor().setType(targetMat);
                toolService.applyMetadataToItem(event.getCursor(), upgraded);
            }
            if (inv.getResult() != null && inv.getResult().getType() != Material.AIR) {
                inv.getResult().setType(targetMat);
                toolService.applyMetadataToItem(inv.getResult(), upgraded);
            }

            String msg = configManager.getTierUpgradedMessage()
                    .replace("%tier%", upgraded.getMaterial().getDisplayName())
                    .replace("%level%", String.valueOf(upgraded.getLevel()));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }
}
