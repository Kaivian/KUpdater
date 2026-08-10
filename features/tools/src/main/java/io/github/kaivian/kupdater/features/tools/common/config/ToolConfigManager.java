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
    private RecoveryConfig recoveryConfig;

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

        this.recoveryConfig = parseRecoveryConfig(config);
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

    /**
     * Returns an unmodifiable set of all tool materials configured in the plugin.
     *
     * @return set of configured ToolMaterial instances
     */
    public java.util.Set<ToolMaterial> getConfiguredMaterials() {
        return java.util.Collections.unmodifiableSet(materialStats.keySet());
    }

    public RecoveryConfig getRecoveryConfig() {
        if (recoveryConfig == null) {
            recoveryConfig = parseRecoveryConfig(null);
        }
        return recoveryConfig;
    }

    private RecoveryConfig parseRecoveryConfig(FileConfiguration config) {
        if (config == null) {
            return createDefaultRecoveryConfig();
        }

        ConfigurationSection recSec = config.getConfigurationSection("recovery");
        if (recSec == null) {
            recSec = config.getConfigurationSection("pickaxe.recovery");
        }
        if (recSec == null) {
            recSec = config.getConfigurationSection("tools.pickaxe.recovery");
        }
        if (recSec == null) {
            return createDefaultRecoveryConfig();
        }

        boolean enabled = recSec.getBoolean("enabled", true);
        String permission = recSec.getString("permission", "kupdater.tool.recover");
        String adminForcePermission = recSec.getString("admin-force-permission", "kupdater.tool.admin.force");
        boolean guiEnabled = recSec.getBoolean("gui.enabled", true);
        String guiTitle = recSec.getString("gui.title", "&8Pickaxe Recovery");
        int guiRows = recSec.getInt("gui.rows", 5);
        boolean confirmOnRecovery = recSec.getBoolean("gui.confirm-on-recovery", true);
        int confirmDelayTicks = recSec.getInt("gui.confirm-delay-ticks", 60);

        int maxRecoveries = recSec.getInt("limits.maximum-recoveries", -1);

        boolean durEnabled = recSec.getBoolean("penalties.durability.enabled", true);
        double durBase = recSec.getDouble("penalties.durability.base-percent", 20.0);
        double durInc = recSec.getDouble("penalties.durability.increment-percent", 10.0);
        double durMax = recSec.getDouble("penalties.durability.maximum-percent", 80.0);

        boolean currEnabled = recSec.getBoolean("penalties.currency.enabled", true);
        double currBase = recSec.getDouble("penalties.currency.base", 500.0);
        double currInc = recSec.getDouble("penalties.currency.increment", 250.0);
        double currMax = recSec.getDouble("penalties.currency.maximum", 5000.0);

        boolean itemsEnabled = recSec.getBoolean("penalties.items.enabled", true);
        double itemsBaseMult = recSec.getDouble("penalties.items.base-multiplier", 1.0);
        double itemsIncMult = recSec.getDouble("penalties.items.increment-multiplier", 0.5);
        double itemsMaxMult = recSec.getDouble("penalties.items.maximum-multiplier", 5.0);

        java.util.Map<String, java.util.List<RecoveryItemRequirement>> tierItemReqs = new java.util.HashMap<>();
        ConfigurationSection tierReqSec = recSec.getConfigurationSection("penalties.items.tier-requirements");
        if (tierReqSec != null) {
            for (String tierKey : tierReqSec.getKeys(false)) {
                java.util.List<RecoveryItemRequirement> tierList = new java.util.ArrayList<>();
                if (tierReqSec.isList(tierKey)) {
                    for (java.util.Map<?, ?> map : tierReqSec.getMapList(tierKey)) {
                        Object matObj = map.get("material");
                        Object amtObj = map.get("amount");
                        if (matObj != null && amtObj != null) {
                            Material mat = parseMaterial(matObj.toString());
                            int amt = parseIntSafe(amtObj.toString(), 1);
                            if (mat != null) {
                                tierList.add(new RecoveryItemRequirement(mat, Math.max(1, amt)));
                            }
                        }
                    }
                }
                if (!tierList.isEmpty()) {
                    tierItemReqs.put(tierKey.toUpperCase(), tierList);
                }
            }
        }
        if (tierItemReqs.isEmpty() && recSec.isList("penalties.items.requirements")) {
            java.util.List<RecoveryItemRequirement> flatReqs = new java.util.ArrayList<>();
            for (java.util.Map<?, ?> map : recSec.getMapList("penalties.items.requirements")) {
                Object matObj = map.get("material");
                Object amtObj = map.get("amount");
                if (matObj != null && amtObj != null) {
                    Material mat = parseMaterial(matObj.toString());
                    int amt = parseIntSafe(amtObj.toString(), 1);
                    if (mat != null) {
                        flatReqs.add(new RecoveryItemRequirement(mat, Math.max(1, amt)));
                    }
                }
            }
            if (!flatReqs.isEmpty()) {
                for (io.github.kaivian.kupdater.api.tools.model.ToolMaterial tm : io.github.kaivian.kupdater.api.tools.model.ToolMaterial.values()) {
                    tierItemReqs.put(tm.name(), flatReqs);
                }
            }
        }
        if (tierItemReqs.isEmpty()) {
            tierItemReqs = createDefaultTierItemRequirements();
        }

        boolean cdEnabled = recSec.getBoolean("penalties.cooldown.enabled", true);
        long cdBase = recSec.getLong("penalties.cooldown.base-seconds", 21600L);
        long cdInc = recSec.getLong("penalties.cooldown.increment-seconds", 7200L);
        long cdMax = recSec.getLong("penalties.cooldown.maximum-seconds", 86400L);

        ConfigurationSection msgSec = recSec.getConfigurationSection("messages");
        String msgNotLost = msgSec != null ? msgSec.getString("not-lost") : null;
        String msgBroken = msgSec != null ? msgSec.getString("broken-cannot-recover") : null;
        String msgCooldown = msgSec != null ? msgSec.getString("on-cooldown") : null;
        String msgFunds = msgSec != null ? msgSec.getString("insufficient-funds") : null;
        String msgItems = msgSec != null ? msgSec.getString("missing-items") : null;
        String msgInvFull = msgSec != null ? msgSec.getString("inventory-full") : null;
        String msgEconUnavail = msgSec != null ? msgSec.getString("economy-unavailable") : null;
        String msgProcessing = msgSec != null ? msgSec.getString("already-processing") : null;
        String msgMaxRec = msgSec != null ? msgSec.getString("max-recoveries") : null;

        return new RecoveryConfig(
                enabled, permission, adminForcePermission, guiEnabled, guiTitle, guiRows, confirmOnRecovery, confirmDelayTicks, maxRecoveries,
                durEnabled, durBase, durInc, durMax,
                currEnabled, currBase, currInc, currMax,
                itemsEnabled, itemsBaseMult, itemsIncMult, itemsMaxMult, tierItemReqs,
                cdEnabled, cdBase, cdInc, cdMax,
                msgNotLost, msgBroken, msgCooldown, msgFunds, msgItems, msgInvFull, msgEconUnavail, msgProcessing, msgMaxRec
        );
    }

    private java.util.Map<String, java.util.List<RecoveryItemRequirement>> createDefaultTierItemRequirements() {
        java.util.Map<String, java.util.List<RecoveryItemRequirement>> map = new java.util.HashMap<>();
        map.put("WOODEN", java.util.Arrays.asList(new RecoveryItemRequirement(Material.OAK_PLANKS, 4)));
        map.put("STONE", java.util.Arrays.asList(new RecoveryItemRequirement(Material.COBBLESTONE, 8)));
        map.put("IRON", java.util.Arrays.asList(new RecoveryItemRequirement(Material.IRON_INGOT, 4)));
        map.put("GOLD", java.util.Arrays.asList(new RecoveryItemRequirement(Material.GOLD_INGOT, 4)));
        map.put("DIAMOND", java.util.Arrays.asList(new RecoveryItemRequirement(Material.DIAMOND, 2)));
        map.put("NETHERITE", java.util.Arrays.asList(
                new RecoveryItemRequirement(Material.DIAMOND, 1)
        ));
        return map;
    }

    private Material parseMaterial(String str) {
        try { return Material.valueOf(str.toUpperCase()); } catch (Throwable ignored) {}
        try { return Material.matchMaterial(str); } catch (Throwable ignored) {}
        return null;
    }

    private int parseIntSafe(String str, int fallback) {
        try { return Integer.parseInt(str); } catch (NumberFormatException e) { return fallback; }
    }

    private RecoveryConfig createDefaultRecoveryConfig() {
        return new RecoveryConfig(
                true, "kupdater.tool.recover", "kupdater.tool.admin.force",
                true, "&8Pickaxe Recovery", 5, true, 60, -1,
                true, 20.0, 10.0, 80.0,
                true, 500.0, 250.0, 5000.0,
                true, 1.0, 0.5, 5.0, createDefaultTierItemRequirements(),
                true, 21600L, 7200L, 86400L,
                null, null, null, null, null, null, null, null, null
        );
    }

    public static class RecoveryItemRequirement {
        private final Material material;
        private final int baseAmount;

        public RecoveryItemRequirement(Material material, int baseAmount) {
            this.material = material;
            this.baseAmount = Math.max(1, baseAmount);
        }

        public Material getMaterial() { return material; }
        public int getBaseAmount() { return baseAmount; }
    }

    public static class RecoveryConfig {
        private final boolean enabled;
        private final String permission;
        private final String adminForcePermission;
        private final boolean guiEnabled;
        private final String guiTitle;
        private final int guiRows;
        private final boolean confirmOnRecovery;
        private final int confirmDelayTicks;
        private final int maximumRecoveries;

        private final boolean durabilityPenaltyEnabled;
        private final double durabilityBasePercent;
        private final double durabilityIncrementPercent;
        private final double durabilityMaximumPercent;

        private final boolean currencyEnabled;
        private final double currencyBase;
        private final double currencyIncrement;
        private final double currencyMaximum;

        private final boolean itemsEnabled;
        private final double itemsBaseMultiplier;
        private final double itemsIncrementMultiplier;
        private final double itemsMaximumMultiplier;
        private final java.util.Map<String, java.util.List<RecoveryItemRequirement>> tierItemRequirements;

        private final boolean cooldownEnabled;
        private final long cooldownBaseSeconds;
        private final long cooldownIncrementSeconds;
        private final long cooldownMaximumSeconds;

        private final String msgNotLost;
        private final String msgBrokenCannotRecover;
        private final String msgOnCooldown;
        private final String msgInsufficientFunds;
        private final String msgMissingItems;
        private final String msgInventoryFull;
        private final String msgEconomyUnavailable;
        private final String msgAlreadyProcessing;
        private final String msgMaxRecoveries;

        public RecoveryConfig(boolean enabled, String permission, String adminForcePermission,
                              boolean guiEnabled, String guiTitle, int guiRows, boolean confirmOnRecovery, int confirmDelayTicks, int maximumRecoveries,
                              boolean durabilityPenaltyEnabled, double durabilityBasePercent, double durabilityIncrementPercent, double durabilityMaximumPercent,
                              boolean currencyEnabled, double currencyBase, double currencyIncrement, double currencyMaximum,
                              boolean itemsEnabled, double itemsBaseMultiplier, double itemsIncrementMultiplier, double itemsMaximumMultiplier,
                              java.util.Map<String, java.util.List<RecoveryItemRequirement>> tierItemRequirements,
                              boolean cooldownEnabled, long cooldownBaseSeconds, long cooldownIncrementSeconds, long cooldownMaximumSeconds,
                              String msgNotLost, String msgBrokenCannotRecover, String msgOnCooldown, String msgInsufficientFunds,
                              String msgMissingItems, String msgInventoryFull, String msgEconomyUnavailable, String msgAlreadyProcessing, String msgMaxRecoveries) {
            this.enabled = enabled;
            this.permission = permission != null ? permission : "kupdater.tool.recover";
            this.adminForcePermission = adminForcePermission != null ? adminForcePermission : "kupdater.tool.admin.force";
            this.guiEnabled = guiEnabled;
            this.guiTitle = guiTitle != null ? guiTitle : "&8Pickaxe Recovery";
            this.guiRows = Math.max(3, Math.min(6, guiRows));
            this.confirmOnRecovery = confirmOnRecovery;
            this.confirmDelayTicks = Math.max(20, Math.min(200, confirmDelayTicks));
            this.maximumRecoveries = maximumRecoveries;

            this.durabilityPenaltyEnabled = durabilityPenaltyEnabled;
            this.durabilityBasePercent = Math.min(100.0, Math.max(0.0, durabilityBasePercent));
            this.durabilityIncrementPercent = Math.max(0.0, durabilityIncrementPercent);
            this.durabilityMaximumPercent = Math.min(100.0, Math.max(this.durabilityBasePercent, durabilityMaximumPercent));

            this.currencyEnabled = currencyEnabled;
            this.currencyBase = Math.max(0.0, currencyBase);
            this.currencyIncrement = Math.max(0.0, currencyIncrement);
            this.currencyMaximum = Math.max(this.currencyBase, currencyMaximum);

            this.itemsEnabled = itemsEnabled;
            this.itemsBaseMultiplier = Math.max(0.0, itemsBaseMultiplier);
            this.itemsIncrementMultiplier = Math.max(0.0, itemsIncrementMultiplier);
            this.itemsMaximumMultiplier = Math.max(this.itemsBaseMultiplier, itemsMaximumMultiplier);
            this.tierItemRequirements = tierItemRequirements != null ? java.util.Collections.unmodifiableMap(tierItemRequirements) : java.util.Collections.emptyMap();

            this.cooldownEnabled = cooldownEnabled;
            this.cooldownBaseSeconds = Math.max(0L, cooldownBaseSeconds);
            this.cooldownIncrementSeconds = Math.max(0L, cooldownIncrementSeconds);
            this.cooldownMaximumSeconds = Math.max(this.cooldownBaseSeconds, cooldownMaximumSeconds);

            this.msgNotLost = msgNotLost != null ? msgNotLost : "&c★ Recovery unavailable: Your pickaxe is not in a LOST state.";
            this.msgBrokenCannotRecover = msgBrokenCannotRecover != null ? msgBrokenCannotRecover : "&c★ Recovery unavailable: Your pickaxe is broken, not lost. Repair it instead!";
            this.msgOnCooldown = msgOnCooldown != null ? msgOnCooldown : "&c★ Recovery is on cooldown! Please wait %remaining%.";
            this.msgInsufficientFunds = msgInsufficientFunds != null ? msgInsufficientFunds : "&c★ You need $%cost% to recover your pickaxe (Balance: $%balance%).";
            this.msgMissingItems = msgMissingItems != null ? msgMissingItems : "&c★ You do not have all required items for recovery.";
            this.msgInventoryFull = msgInventoryFull != null ? msgInventoryFull : "&c★ Recovery unavailable: You need at least 1 free inventory slot.";
            this.msgEconomyUnavailable = msgEconomyUnavailable != null ? msgEconomyUnavailable : "&c★ Recovery unavailable: Economy provider (Vault) is currently disabled.";
            this.msgAlreadyProcessing = msgAlreadyProcessing != null ? msgAlreadyProcessing : "&c★ Recovery transaction is already processing. Please wait.";
            this.msgMaxRecoveries = msgMaxRecoveries != null ? msgMaxRecoveries : "&c★ You have reached the maximum allowed recoveries (%max%).";
        }

        public boolean isEnabled() { return enabled; }
        public String getPermission() { return permission; }
        public String getAdminForcePermission() { return adminForcePermission; }
        public boolean isGuiEnabled() { return guiEnabled; }
        public String getGuiTitle() { return guiTitle; }
        public int getGuiRows() { return guiRows; }
        public boolean isConfirmOnRecovery() { return confirmOnRecovery; }
        public int getConfirmDelayTicks() { return confirmDelayTicks; }
        public int getMaximumRecoveries() { return maximumRecoveries; }

        public boolean isDurabilityPenaltyEnabled() { return durabilityPenaltyEnabled; }
        public double getDurabilityBasePercent() { return durabilityBasePercent; }
        public double getDurabilityIncrementPercent() { return durabilityIncrementPercent; }
        public double getDurabilityMaximumPercent() { return durabilityMaximumPercent; }

        public boolean isCurrencyEnabled() { return currencyEnabled; }
        public double getCurrencyBase() { return currencyBase; }
        public double getCurrencyIncrement() { return currencyIncrement; }
        public double getCurrencyMaximum() { return currencyMaximum; }

        public boolean isItemsEnabled() { return itemsEnabled; }
        public double getItemsBaseMultiplier() { return itemsBaseMultiplier; }
        public double getItemsIncrementMultiplier() { return itemsIncrementMultiplier; }
        public double getItemsMaximumMultiplier() { return itemsMaximumMultiplier; }
        public java.util.Map<String, java.util.List<RecoveryItemRequirement>> getTierItemRequirements() { return tierItemRequirements; }
        public java.util.List<RecoveryItemRequirement> getItemRequirementsForTier(String tierName) {
            return tierItemRequirements.getOrDefault(tierName != null ? tierName.toUpperCase() : "", java.util.Collections.emptyList());
        }

        public boolean isCooldownEnabled() { return cooldownEnabled; }
        public long getCooldownBaseSeconds() { return cooldownBaseSeconds; }
        public long getCooldownIncrementSeconds() { return cooldownIncrementSeconds; }
        public long getCooldownMaximumSeconds() { return cooldownMaximumSeconds; }

        public String getMsgNotLost() { return msgNotLost; }
        public String getMsgBrokenCannotRecover() { return msgBrokenCannotRecover; }
        public String getMsgOnCooldown() { return msgOnCooldown; }
        public String getMsgInsufficientFunds() { return msgInsufficientFunds; }
        public String getMsgMissingItems() { return msgMissingItems; }
        public String getMsgInventoryFull() { return msgInventoryFull; }
        public String getMsgEconomyUnavailable() { return msgEconomyUnavailable; }
        public String getMsgAlreadyProcessing() { return msgAlreadyProcessing; }
        public String getMsgMaxRecoveries() { return msgMaxRecoveries; }
    }
}
