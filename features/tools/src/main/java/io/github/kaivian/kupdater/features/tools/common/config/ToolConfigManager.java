package io.github.kaivian.kupdater.features.tools.common.config;

import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolStat;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages configuration loading, parsing, and caching for Tool Updater material tiers, level stats, and block XP.
 */
public class ToolConfigManager {

    private final Logger logger;
    public static class UpgradeRecipe {
        private final Material material;
        private final int amount;

        public UpgradeRecipe(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        public Material getMaterial() {
            return material;
        }

        public int getAmount() {
            return amount;
        }
    }

    private final Map<ToolMaterial, Map<Integer, ToolStat>> materialStats = new EnumMap<>(ToolMaterial.class);
    private final Map<ToolMaterial, Integer> materialMaxLevels = new EnumMap<>(ToolMaterial.class);
    private final Map<ToolMaterial, ToolMaterial> materialNextTiers = new EnumMap<>(ToolMaterial.class);
    private final Map<ToolMaterial, UpgradeRecipe> materialUpgradeRecipes = new EnumMap<>(ToolMaterial.class);
    private final Map<ToolMaterial, Integer> materialCustomModelData = new EnumMap<>(ToolMaterial.class);
    private final Map<ToolMaterial, Material> materialBukkitMaterials = new EnumMap<>(ToolMaterial.class);
    private final Map<Material, Integer> blockXpMap = new EnumMap<>(Material.class);
    private final java.util.Set<Material> pickaxeMineableBlocks = new java.util.HashSet<>();

    private int defaultBlockXp = 1;
    private String defaultOwnershipDenyMessage = "&cThis Pickaxe does not belong to you.";
    private String defaultToolBrokenMessage = "&cThis Pickaxe is broken and cannot be used.";
    private String defaultToolInvalidMessage = "&cThis Pickaxe carries an unknown tool ID and cannot be used.";
    private String toolLostMessage = "&cThis Pickaxe is lost and cannot be used.";
    private String alreadyOwnPickaxeMessage = "&c★ You already own a KUpdater Pickaxe! If you lost your pickaxe, use &e/kup recover&c to claim it back.";
    private String firstPickaxeCraftedMessage = "&a★ Congratulations! You successfully crafted your first KUpdater Wooden Pickaxe. Mine blocks to gain XP and upgrade your tool!";
    private String vanillaCraftingDisabledMessage = "&c★ Vanilla pickaxe crafting is disabled! Upgrade your Wooden Pickaxe through tier progression.";
    private String notMaxLevelUpgradeMessage = "&cThis Pickaxe is not at max level or max XP for tier upgrade.";
    private String notMaxLevelUpgradeSmithingMessage = "&cThis Diamond Pickaxe is not at max level or max XP for Netherite upgrade.";
    private String tierUpgradedMessage = "&a★ Upgraded Pickaxe Tier to &e%tier% Level %level%&a!";
    private String levelUpgradedMessage = "&a▲ Your Pickaxe upgraded to &e%tier% Level %level%&a!";
    private String recoverNoPickaxeMessage = "&c★ You do not own a KUpdater Pickaxe yet! Craft a Wooden Pickaxe for the first time.";
    private String recoverAlreadyInInventoryMessage = "&c★ Your KUpdater Pickaxe is already in your inventory!";
    private String recoverSuccessMessage = "&a★ Recovery successful! You received your &e%tier% Level %level%&a.";
    private String reloadSuccessMessage = "&a★ [KUpdater] Configuration reloaded successfully in %duration%ms!";
    private String noPermissionMessage = "&cYou do not have permission to execute this command.";

    private String displayNameTemplate = "&f%state% %material% Pickaxe &8(Lvl %level%)";
    private String brokenDisplayNameTemplate = "&cBroken %material% Pickaxe";
    private String lostDisplayNameTemplate = "&e%material% Pickaxe (Lost)";
    private java.util.List<String> loreFormatList = new java.util.ArrayList<>();

    private String progressBarSymbol = " ";
    private int progressBarLength = 22;
    private String progressBarCompletedColor = "&a&l&m";
    private String progressBarRemainingColor = "&8&l&m";

    private boolean allowOverflowXpPool = true;
    private int maxOverflowXpPool = 10000;

    private String maxLevelGuideCrafting = "&e⚡ Max tier level reached! Upgrade with &f%upgrade_amount% %upgrade_item%&e in Crafting Table.";
    private String maxLevelGuideSmithing = "&e⚡ Max tier level reached! Upgrade with &f%upgrade_amount% %upgrade_item%&e in Smithing Table.";
    private String maxLevelReachedText = "&a&l✦ MAX LEVEL REACHED ✦";
    private String storedXpText = "&7Stored XP: &e%stored_xp%";
    private int loreMaxLineLength = 35;

    public ToolConfigManager(Logger logger) {
        this.logger = logger != null ? logger : Logger.getLogger("ToolConfigManager");
        loadDefaultLoreFormatList();
    }

    /**
     * Loads level stats, block XP mappings, and settings from configuration.
     *
     * @param config FileConfiguration instance
     */
    public void load(FileConfiguration config) {
        load(null, config);
    }

    public void load(FileConfiguration sharedConfig, FileConfiguration pickaxeConfig) {
        materialStats.clear();
        materialMaxLevels.clear();
        materialNextTiers.clear();
        blockXpMap.clear();
        pickaxeMineableBlocks.clear();

        FileConfiguration config = pickaxeConfig != null ? pickaxeConfig : sharedConfig;

        if (config == null) {
            loadDefaultMaterialTierStats();
            loadDefaultBlockXpMap();
            loadDefaultLoreFormatList();
            return;
        }

        ConfigurationSection pickaxeSec = config.getConfigurationSection("pickaxe");
        if (pickaxeSec == null) {
            pickaxeSec = config.getConfigurationSection("tools.pickaxe");
        }
        if (pickaxeSec == null) {
            pickaxeSec = config;
        }

        this.defaultOwnershipDenyMessage = config.getString(
                "deny-message",
                config.getString("pickaxe.deny-message",
                config.getString("tools.pickaxe.deny-message", defaultOwnershipDenyMessage))
        );
        this.defaultToolBrokenMessage = config.getString(
                "broken-message",
                config.getString("pickaxe.broken-message",
                config.getString("tools.pickaxe.broken-message", defaultToolBrokenMessage))
        );
        this.defaultToolInvalidMessage = config.getString(
                "invalid-message",
                config.getString("pickaxe.invalid-message",
                config.getString("tools.pickaxe.invalid-message", defaultToolInvalidMessage))
        );

        ConfigurationSection msgSec = pickaxeSec != null ? pickaxeSec.getConfigurationSection("messages") : null;
        if (msgSec == null && config != null) {
            msgSec = config.getConfigurationSection("messages");
        }

        if (msgSec != null) {
            this.defaultOwnershipDenyMessage = msgSec.getString("deny-ownership", defaultOwnershipDenyMessage);
            this.defaultToolBrokenMessage = msgSec.getString("tool-broken", defaultToolBrokenMessage);
            this.defaultToolInvalidMessage = msgSec.getString("tool-invalid", defaultToolInvalidMessage);
            this.toolLostMessage = msgSec.getString("tool-lost", toolLostMessage);
            this.alreadyOwnPickaxeMessage = msgSec.getString("already-own-pickaxe", alreadyOwnPickaxeMessage);
            this.firstPickaxeCraftedMessage = msgSec.getString("first-pickaxe-crafted", firstPickaxeCraftedMessage);
            this.vanillaCraftingDisabledMessage = msgSec.getString("vanilla-crafting-disabled", vanillaCraftingDisabledMessage);
            this.notMaxLevelUpgradeMessage = msgSec.getString("not-max-level-upgrade", notMaxLevelUpgradeMessage);
            this.notMaxLevelUpgradeSmithingMessage = msgSec.getString("not-max-level-upgrade-smithing", notMaxLevelUpgradeSmithingMessage);
            this.tierUpgradedMessage = msgSec.getString("tier-upgraded", tierUpgradedMessage);
            this.levelUpgradedMessage = msgSec.getString("level-upgraded", levelUpgradedMessage);
            this.recoverNoPickaxeMessage = msgSec.getString("recover-no-pickaxe", recoverNoPickaxeMessage);
            this.recoverAlreadyInInventoryMessage = msgSec.getString("recover-already-in-inventory", recoverAlreadyInInventoryMessage);
            this.recoverSuccessMessage = msgSec.getString("recover-success", recoverSuccessMessage);
            this.reloadSuccessMessage = msgSec.getString("reload-success", reloadSuccessMessage);
            this.noPermissionMessage = msgSec.getString("no-permission", noPermissionMessage);
        }

        if (pickaxeSec != null) {
            this.allowOverflowXpPool = pickaxeSec.getBoolean("allow-overflow-xp-pool", true);
            this.maxOverflowXpPool = pickaxeSec.getInt("max-overflow-xp-pool", 10000);
            this.loreMaxLineLength = pickaxeSec.getInt("lore-max-line-length", 35);
            this.displayNameTemplate = pickaxeSec.getString("display-name", "&f%material% Pickaxe &8(Lvl %level%)");
            this.brokenDisplayNameTemplate = pickaxeSec.getString("broken-display-name", "&cBroken %material% Pickaxe");
            this.lostDisplayNameTemplate = pickaxeSec.getString("lost-display-name", "&e%material% Pickaxe (Lost)");

            if (pickaxeSec.isList("lore")) {
                this.loreFormatList = pickaxeSec.getStringList("lore");
            } else {
                loadDefaultLoreFormatList();
            }

            ConfigurationSection progressBarSec = pickaxeSec.getConfigurationSection("progress-bar");
            if (progressBarSec != null) {
                this.progressBarSymbol = progressBarSec.getString("symbol", "|");
                this.progressBarLength = progressBarSec.getInt("length", 20);
                this.progressBarCompletedColor = progressBarSec.getString("completed-color", "&a");
                this.progressBarRemainingColor = progressBarSec.getString("remaining-color", "&7");
            }

            ConfigurationSection guideSec = pickaxeSec.getConfigurationSection("max-level-guide");
            if (guideSec != null) {
                this.maxLevelGuideCrafting = guideSec.getString("crafting", "&e⚡ Max tier level reached! Upgrade with &f%upgrade_amount% %upgrade_item%&e in Crafting Table.");
                this.maxLevelGuideSmithing = guideSec.getString("smithing", "&e⚡ Max tier level reached! Upgrade with &f%upgrade_amount% %upgrade_item%&e in Smithing Table.");
            }

            this.maxLevelReachedText = pickaxeSec.getString("max-level-reached-text", "&a&l✦ MAX LEVEL REACHED ✦");
            this.storedXpText = pickaxeSec.getString("stored-xp-text", "&7Stored XP: &e%stored_xp%");

            if (pickaxeSec.isList("mineable-blocks")) {
                for (String entry : pickaxeSec.getStringList("mineable-blocks")) {
                    try {
                        Material mat = Material.matchMaterial(entry);
                        if (mat != null) {
                            pickaxeMineableBlocks.add(mat);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            loadDefaultLoreFormatList();
        }

        // Load Block XP values
        ConfigurationSection blockXpSec = pickaxeSec != null ? pickaxeSec.getConfigurationSection("block-xp") : null;
        if (blockXpSec != null) {
            this.defaultBlockXp = blockXpSec.getInt("default", 1);
            for (String key : blockXpSec.getKeys(false)) {
                if (key.equalsIgnoreCase("default")) continue;
                try {
                    Material mat = Material.matchMaterial(key);
                    if (mat != null) {
                        int xpVal = blockXpSec.getInt(key, defaultBlockXp);
                        blockXpMap.put(mat, Math.max(0, xpVal));
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (blockXpMap.isEmpty()) {
            loadDefaultBlockXpMap();
        }

        ConfigurationSection tiersSec = pickaxeSec != null ? pickaxeSec.getConfigurationSection("tiers") : null;
        if (tiersSec != null) {
            for (String matKey : tiersSec.getKeys(false)) {
                ToolMaterial material = ToolMaterial.fromString(matKey);
                ConfigurationSection tierConfig = tiersSec.getConfigurationSection(matKey);
                if (tierConfig == null) continue;

                int maxLvl = tierConfig.getInt("max-level", 3);
                String nextTierStr = tierConfig.getString("next-tier", null);
                ToolMaterial nextTier = nextTierStr != null && !nextTierStr.equalsIgnoreCase("null") ? ToolMaterial.fromString(nextTierStr) : null;

                ConfigurationSection levelsSec = tierConfig.getConfigurationSection("levels");
                Map<Integer, ToolStat> levelMap = new HashMap<>();

                if (levelsSec != null) {
                    for (String lvlKey : levelsSec.getKeys(false)) {
                        try {
                            int lvl = Integer.parseInt(lvlKey);
                            double speed = levelsSec.getDouble(lvlKey + ".mining-speed", 1.0 + (lvl - 1) * 0.15);
                            int dura = levelsSec.getInt(lvlKey + ".durability", 59 + (lvl - 1) * 20);
                            int reqXp = levelsSec.getInt(lvlKey + ".required-xp", 50 * lvl);
                            levelMap.put(lvl, new ToolStat(speed, dura, reqXp));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                ConfigurationSection recipeSec = tierConfig.getConfigurationSection("upgrade-recipe");
                if (recipeSec != null) {
                    String matName = recipeSec.getString("material", "STONE");
                    int amt = recipeSec.getInt("amount", 8);
                    Material mat = Material.matchMaterial(matName);
                    if (mat != null) {
                        materialUpgradeRecipes.put(material, new UpgradeRecipe(mat, Math.max(1, amt)));
                    }
                }

                String itemMatStr = tierConfig.getString("item-material", null);
                if (itemMatStr != null) {
                    Material itemMat = Material.matchMaterial(itemMatStr);
                    if (itemMat != null) {
                        materialBukkitMaterials.put(material, itemMat);
                    } else {
                        materialBukkitMaterials.put(material, material.getBukkitMaterial());
                    }
                }

                int cmd = tierConfig.getInt("custom-model-data", 0);
                if (cmd > 0) {
                    materialCustomModelData.put(material, cmd);
                }

                if (levelMap.isEmpty()) {
                    levelMap.put(1, new ToolStat(1.00, 59, 50));
                }

                materialStats.put(material, levelMap);
                materialMaxLevels.put(material, Math.max(1, maxLvl));
                if (nextTier != null && nextTier != material) {
                    materialNextTiers.put(material, nextTier);
                }
            }
        }

        if (materialStats.isEmpty()) {
            loadDefaultMaterialTierStats();
        }
    }

    public Material getBukkitMaterial(ToolMaterial material) {
        if (material == null) return Material.WOODEN_PICKAXE;
        return materialBukkitMaterials.getOrDefault(material, material.getBukkitMaterial());
    }

    public int getCustomModelData(ToolMaterial material) {
        return materialCustomModelData.getOrDefault(material, 0);
    }

    private void loadDefaultMaterialTierStats() {
        // Wooden Tier (3 Levels -> Next: Stone)
        Map<Integer, ToolStat> wooden = new HashMap<>();
        wooden.put(1, new ToolStat(2.00, 59, 50));
        wooden.put(2, new ToolStat(2.60, 75, 100));
        wooden.put(3, new ToolStat(3.20, 95, 150));
        materialStats.put(ToolMaterial.WOODEN, wooden);
        materialMaxLevels.put(ToolMaterial.WOODEN, 3);
        materialNextTiers.put(ToolMaterial.WOODEN, ToolMaterial.STONE);
        materialUpgradeRecipes.put(ToolMaterial.WOODEN, new UpgradeRecipe(Material.STONE, 8));

        // Stone Tier (3 Levels -> Next: Copper)
        Map<Integer, ToolStat> stone = new HashMap<>();
        stone.put(1, new ToolStat(4.00, 131, 200));
        stone.put(2, new ToolStat(4.30, 160, 300));
        stone.put(3, new ToolStat(4.60, 190, 450));
        materialStats.put(ToolMaterial.STONE, stone);
        materialMaxLevels.put(ToolMaterial.STONE, 3);
        materialNextTiers.put(ToolMaterial.STONE, ToolMaterial.COPPER);
        materialUpgradeRecipes.put(ToolMaterial.STONE, new UpgradeRecipe(Material.COPPER_INGOT, 8));

        // Copper Tier (3 Levels -> Next: Iron)
        Map<Integer, ToolStat> copper = new HashMap<>();
        copper.put(1, new ToolStat(5.00, 200, 500));
        copper.put(2, new ToolStat(5.30, 230, 650));
        copper.put(3, new ToolStat(5.60, 260, 800));
        materialStats.put(ToolMaterial.COPPER, copper);
        materialMaxLevels.put(ToolMaterial.COPPER, 3);
        materialNextTiers.put(ToolMaterial.COPPER, ToolMaterial.IRON);
        materialUpgradeRecipes.put(ToolMaterial.COPPER, new UpgradeRecipe(Material.IRON_INGOT, 8));

        // Iron Tier (4 Levels -> Next: Gold)
        Map<Integer, ToolStat> iron = new HashMap<>();
        iron.put(1, new ToolStat(6.00, 250, 900));
        iron.put(2, new ToolStat(6.25, 290, 1100));
        iron.put(3, new ToolStat(6.50, 330, 1350));
        iron.put(4, new ToolStat(6.75, 370, 1650));
        materialStats.put(ToolMaterial.IRON, iron);
        materialMaxLevels.put(ToolMaterial.IRON, 4);
        materialNextTiers.put(ToolMaterial.IRON, ToolMaterial.GOLD);
        materialUpgradeRecipes.put(ToolMaterial.IRON, new UpgradeRecipe(Material.GOLD_INGOT, 8));

        // Gold Tier (4 Levels -> Next: Diamond)
        Map<Integer, ToolStat> gold = new HashMap<>();
        gold.put(1, new ToolStat(7.00, 150, 1800));
        gold.put(2, new ToolStat(7.25, 200, 2200));
        gold.put(3, new ToolStat(7.50, 250, 2700));
        gold.put(4, new ToolStat(7.75, 300, 3300));
        materialStats.put(ToolMaterial.GOLD, gold);
        materialMaxLevels.put(ToolMaterial.GOLD, 4);
        materialNextTiers.put(ToolMaterial.GOLD, ToolMaterial.DIAMOND);
        materialUpgradeRecipes.put(ToolMaterial.GOLD, new UpgradeRecipe(Material.DIAMOND, 8));

        // Diamond Tier (5 Levels -> Next: Netherite, upgraded via Smithing Table)
        Map<Integer, ToolStat> diamond = new HashMap<>();
        diamond.put(1, new ToolStat(8.00, 1561, 3800));
        diamond.put(2, new ToolStat(8.15, 1750, 4500));
        diamond.put(3, new ToolStat(8.30, 1950, 5300));
        diamond.put(4, new ToolStat(8.45, 2150, 6200));
        diamond.put(5, new ToolStat(8.60, 2400, 7200));
        materialStats.put(ToolMaterial.DIAMOND, diamond);
        materialMaxLevels.put(ToolMaterial.DIAMOND, 5);
        materialNextTiers.put(ToolMaterial.DIAMOND, ToolMaterial.NETHERITE);

        // Netherite Tier (5 Levels -> Max)
        Map<Integer, ToolStat> netherite = new HashMap<>();
        netherite.put(1, new ToolStat(9.00, 2031, 8000));
        netherite.put(2, new ToolStat(9.50, 2300, 9000));
        netherite.put(3, new ToolStat(10.00, 2600, 10500));
        netherite.put(4, new ToolStat(10.50, 2900, 12000));
        netherite.put(5, new ToolStat(11.00, 3300, 14000));
        materialStats.put(ToolMaterial.NETHERITE, netherite);
        materialMaxLevels.put(ToolMaterial.NETHERITE, 5);
    }

    private void loadDefaultBlockXpMap() {
        blockXpMap.clear();
        defaultBlockXp = 1;
        setBlockXp("STONE", 1);
        setBlockXp("COAL_ORE", 3);
        setBlockXp("DEEPSLATE_COAL_ORE", 3);
        setBlockXp("COPPER_ORE", 3);
        setBlockXp("DEEPSLATE_COPPER_ORE", 3);
        setBlockXp("IRON_ORE", 5);
        setBlockXp("DEEPSLATE_IRON_ORE", 5);
        setBlockXp("GOLD_ORE", 8);
        setBlockXp("DEEPSLATE_GOLD_ORE", 8);
        setBlockXp("REDSTONE_ORE", 6);
        setBlockXp("DEEPSLATE_REDSTONE_ORE", 6);
        setBlockXp("LAPIS_ORE", 6);
        setBlockXp("DEEPSLATE_LAPIS_ORE", 6);
        setBlockXp("DIAMOND_ORE", 15);
        setBlockXp("DEEPSLATE_DIAMOND_ORE", 15);
        setBlockXp("EMERALD_ORE", 20);
        setBlockXp("DEEPSLATE_EMERALD_ORE", 20);
        setBlockXp("NETHER_QUARTZ_ORE", 4);
        setBlockXp("NETHER_GOLD_ORE", 4);
        setBlockXp("ANCIENT_DEBRIS", 25);
        setBlockXp("OBSIDIAN", 10);
    }

    private void setBlockXp(String materialName, int xp) {
        try {
            Material mat = Material.matchMaterial(materialName);
            if (mat != null) {
                blockXpMap.put(mat, xp);
            }
        } catch (Exception ignored) {
        }
    }

    public int getBlockXp(Material material) {
        if (material == null) return 0;
        return blockXpMap.getOrDefault(material, defaultBlockXp);
    }

    public ToolStat getStat(ToolType toolType, int level) {
        return getStat(ToolMaterial.WOODEN, level);
    }

    public ToolStat getStat(ToolMaterial material, int level) {
        Map<Integer, ToolStat> stats = materialStats.get(material != null ? material : ToolMaterial.WOODEN);
        if (stats == null || stats.isEmpty()) {
            return new ToolStat(1.0, 59, 50);
        }
        ToolStat stat = stats.get(level);
        if (stat != null) {
            return stat;
        }
        int bestLevel = 1;
        for (int l : stats.keySet()) {
            if (l <= level && l > bestLevel) {
                bestLevel = l;
            }
        }
        return stats.getOrDefault(bestLevel, new ToolStat(1.0, 59, 50));
    }

    public int getMaxLevel(ToolType toolType) {
        return getMaxLevel(ToolMaterial.WOODEN);
    }

    public int getMaxLevel(ToolMaterial material) {
        return materialMaxLevels.getOrDefault(material != null ? material : ToolMaterial.WOODEN, 3);
    }

    public ToolMaterial getNextTier(ToolMaterial material) {
        if (material == null) return null;
        return materialNextTiers.get(material);
    }

    public UpgradeRecipe getUpgradeRecipe(ToolMaterial material) {
        if (material == null) return null;
        return materialUpgradeRecipes.get(material);
    }

    public String getOwnershipDenyMessage() {
        return defaultOwnershipDenyMessage;
    }

    public String getToolBrokenMessage() {
        return defaultToolBrokenMessage;
    }

    public String getInvalidToolMessage() {
        return defaultToolInvalidMessage;
    }

    public String getToolLostMessage() {
        return toolLostMessage;
    }

    public String getAlreadyOwnPickaxeMessage() {
        return alreadyOwnPickaxeMessage;
    }

    public String getFirstPickaxeCraftedMessage() {
        return firstPickaxeCraftedMessage;
    }

    public String getVanillaCraftingDisabledMessage() {
        return vanillaCraftingDisabledMessage;
    }

    public String getNotMaxLevelUpgradeMessage() {
        return notMaxLevelUpgradeMessage;
    }

    public String getNotMaxLevelUpgradeSmithingMessage() {
        return notMaxLevelUpgradeSmithingMessage;
    }

    public String getTierUpgradedMessage() {
        return tierUpgradedMessage;
    }

    public String getLevelUpgradedMessage() {
        return levelUpgradedMessage;
    }

    public String getRecoverNoPickaxeMessage() {
        return recoverNoPickaxeMessage;
    }

    public String getRecoverAlreadyInInventoryMessage() {
        return recoverAlreadyInInventoryMessage;
    }

    public String getRecoverSuccessMessage() {
        return recoverSuccessMessage;
    }

    public String getReloadSuccessMessage() {
        return reloadSuccessMessage;
    }

    public String getNoPermissionMessage() {
        return noPermissionMessage;
    }

    private void loadDefaultLoreFormatList() {
        this.loreFormatList = new java.util.ArrayList<>(java.util.Arrays.asList(
                "",
                "&7Mining Speed: &a%speed%",
                "&7Durability: &f%current%/%max%",
                "",
                "&7Progress to Level %next_level%: &a%percentage%%",
                "%progress_bar% &a%xp%&7/&a%max_xp% &7XP"
        ));
    }

    public String getDisplayNameTemplate() {
        return displayNameTemplate;
    }

    public String getBrokenDisplayNameTemplate() {
        return brokenDisplayNameTemplate;
    }

    public String getLostDisplayNameTemplate() {
        return lostDisplayNameTemplate;
    }

    public java.util.List<String> getLoreFormatList() {
        return loreFormatList;
    }

    public String getProgressBarSymbol() {
        return progressBarSymbol;
    }

    public int getProgressBarLength() {
        return progressBarLength;
    }

    public String getProgressBarCompletedColor() {
        return progressBarCompletedColor;
    }

    public String getProgressBarRemainingColor() {
        return progressBarRemainingColor;
    }

    public boolean isAllowOverflowXpPool() {
        return allowOverflowXpPool;
    }

    public int getMaxOverflowXpPool() {
        return maxOverflowXpPool;
    }

    public String getMaxLevelGuideCrafting() {
        return maxLevelGuideCrafting;
    }

    public String getMaxLevelGuideSmithing() {
        return maxLevelGuideSmithing;
    }

    public int getLoreMaxLineLength() {
        return loreMaxLineLength;
    }

    public String getMaxLevelReachedText() {
        return maxLevelReachedText;
    }

    public String getStoredXpText() {
        return storedXpText;
    }

    /**
     * Checks whether the given block material is listed in the {@code mineable-blocks}
     * configuration for Pickaxe Mining Speed bonus eligibility.
     *
     * @param material the block material to check
     * @return true if the block is in the configured mineable-blocks list
     */
    public boolean isPickaxeMineableBlock(Material material) {
        if (material == null || material.isAir()) return false;
        return pickaxeMineableBlocks.contains(material);
    }
}
