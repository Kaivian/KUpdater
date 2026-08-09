package io.github.kaivian.kupdater.features.tools.common.recipe;

import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolStat;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager.UpgradeRecipe;
import io.github.kaivian.kupdater.features.tools.common.metadata.ToolItemMetadataService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registers Bukkit ShapedRecipes for Pickaxe Tier Upgrades and Starter Wooden Pickaxe in Crafting Table.
 * Pre-stamps KUpdater metadata on recipe result ItemStacks so Recipe Book and Crafting Table previews
 * display custom display names, custom lore, and custom max durability.
 */
public class ToolRecipeManager {

    private final Plugin plugin;
    private final ToolConfigManager configManager;
    private final ToolItemMetadataService metadataService;
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public ToolRecipeManager(Plugin plugin, ToolConfigManager configManager, ToolItemMetadataService metadataService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.metadataService = metadataService;
    }

    public ToolRecipeManager(Plugin plugin, ToolConfigManager configManager) {
        this(plugin, configManager, null);
    }

    public void registerUpgradeRecipes() {
        // 1. Register KUpdater Starter Wooden Pickaxe Recipe
        registerStarterWoodenPickaxeRecipe();

        // 2. Register Tier Upgrade Recipes
        for (ToolMaterial tier : ToolMaterial.values()) {
            ToolMaterial nextTier = configManager.getNextTier(tier);
            if (nextTier == null) continue;

            UpgradeRecipe recipe = configManager.getUpgradeRecipe(tier);
            if (recipe == null) continue;

            Material centerPickaxeMat = configManager.getBukkitMaterial(tier);
            Material surroundingMat = recipe.getMaterial();

            ItemStack resultItem = createPreviewItem(nextTier);

            String keyName = "upgrade_" + tier.name().toLowerCase() + "_to_" + nextTier.name().toLowerCase();
            createAndAddShapedRecipe(keyName, resultItem, centerPickaxeMat, surroundingMat);

            if (tier == ToolMaterial.WOODEN && (surroundingMat == Material.STONE || surroundingMat == Material.COBBLESTONE)) {
                createAndAddShapedRecipe(keyName + "_cobble", createPreviewItem(nextTier), centerPickaxeMat, Material.COBBLESTONE);
                createAndAddShapedRecipe(keyName + "_mossy", createPreviewItem(nextTier), centerPickaxeMat, Material.MOSSY_COBBLESTONE);
            }
        }
    }

    private ItemStack createPreviewItem(ToolMaterial tier) {
        Material bukkitMat = configManager != null ? configManager.getBukkitMaterial(tier) : Material.valueOf(tier.name() + "_PICKAXE");
        ItemStack item = new ItemStack(bukkitMat, 1);

        ToolStat lvl1Stat = configManager != null ? configManager.getStat(tier, 1) : null;
        int maxDurability = lvl1Stat != null ? lvl1Stat.getMaxDurability() : 59;
        int requiredXp = lvl1Stat != null ? lvl1Stat.getRequiredXp() : 50;
        double speed = lvl1Stat != null ? lvl1Stat.getMiningSpeedMultiplier() : 2.0;

        ToolProgression previewProg = new ToolProgression(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                ToolType.PICKAXE,
                tier,
                1, 0, 0,
                ToolState.ACTIVE,
                maxDurability, 1,
                Instant.now(), Instant.now()
        );

        if (metadataService != null) {
            metadataService.stampMetadata(item, previewProg, maxDurability, requiredXp, speed);
        }
        return item;
    }

    private void registerStarterWoodenPickaxeRecipe() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "starter_wooden_pickaxe");
            if (Bukkit.getRecipe(key) != null) {
                if (!registeredKeys.contains(key)) {
                    registeredKeys.add(key);
                }
                return;
            }

            ItemStack starterItem = createPreviewItem(ToolMaterial.WOODEN);
            ShapedRecipe recipe = new ShapedRecipe(key, starterItem);
            recipe.shape("PPP", " S ", " S ");

            List<Material> planks = new ArrayList<>();
            for (Material mat : Material.values()) {
                if (mat.name().endsWith("_PLANKS")) {
                    planks.add(mat);
                }
            }
            if (planks.isEmpty()) planks.add(Material.OAK_PLANKS);

            recipe.setIngredient('P', new RecipeChoice.MaterialChoice(planks));
            recipe.setIngredient('S', Material.STICK);

            Bukkit.addRecipe(recipe);
            registeredKeys.add(key);
            plugin.getLogger().info("[ToolRecipeManager] Registered starter Wooden Pickaxe recipe with KUpdater metadata.");
        } catch (Throwable t) {
            plugin.getLogger().warning("[ToolRecipeManager] Could not register starter Wooden Pickaxe recipe: " + t.getMessage());
        }
    }

    private void createAndAddShapedRecipe(String keyName, ItemStack resultItem, Material centerPickaxeMat, Material surroundingMat) {
        try {
            NamespacedKey key = new NamespacedKey(plugin, keyName);
            if (Bukkit.getRecipe(key) != null) {
                if (!registeredKeys.contains(key)) {
                    registeredKeys.add(key);
                }
                return;
            }
            ShapedRecipe recipe = new ShapedRecipe(key, resultItem);
            recipe.shape("SSS", "SPS", "SSS");
            recipe.setIngredient('S', surroundingMat);
            recipe.setIngredient('P', centerPickaxeMat);

            Bukkit.addRecipe(recipe);
            registeredKeys.add(key);
            plugin.getLogger().info("[ToolRecipeManager] Registered recipe: " + keyName);
        } catch (Throwable t) {
            plugin.getLogger().warning("[ToolRecipeManager] Failed to register recipe " + keyName + ": " + t.getMessage());
        }
    }

    public void unregisterUpgradeRecipes() {
        for (NamespacedKey key : registeredKeys) {
            try {
                if (Bukkit.getRecipe(key) != null) {
                    Bukkit.removeRecipe(key);
                }
            } catch (Throwable ignored) {
            }
        }
        registeredKeys.clear();
    }
}
