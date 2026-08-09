package io.github.kaivian.kupdater.features.tools.common.metadata;

import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager.UpgradeRecipe;

/**
 * Handles PDC persistent metadata stamping, reading, and presentation lore formatting.
 */
public class ToolItemMetadataService {

    private final Plugin plugin;
    private final NamespacedKey keyToolUuid;
    private final NamespacedKey keyOwnerUuid;
    private final NamespacedKey keyToolType;
    private final NamespacedKey keyMaterial;
    private final NamespacedKey keySchemaVersion;
    private final NamespacedKey keyMiningSpeed;
    private final NamespacedKey keyOverflowXp;
    private final ToolConfigManager configManager;

    public ToolItemMetadataService(Plugin plugin) {
        this(plugin, null);
    }

    public ToolItemMetadataService(Plugin plugin, ToolConfigManager configManager) {
        this.plugin = plugin;
        String namespace = plugin != null ? plugin.getName().toLowerCase() : "kupdater";
        this.keyToolUuid = new NamespacedKey(namespace, "tool_uuid");
        this.keyOwnerUuid = new NamespacedKey(namespace, "owner_uuid");
        this.keyToolType = new NamespacedKey(namespace, "tool_type");
        this.keyMaterial = new NamespacedKey(namespace, "material");
        this.keySchemaVersion = new NamespacedKey(namespace, "schema_version");
        this.keyMiningSpeed = new NamespacedKey(namespace, "mining_speed_attr");
        this.keyOverflowXp = new NamespacedKey(namespace, "overflow_xp");
        this.configManager = configManager;
    }

    public boolean isManagedTool(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(keyToolUuid, PersistentDataType.STRING);
    }

    public Optional<UUID> getToolUuid(ItemStack itemStack) {
        if (!isManagedTool(itemStack)) return Optional.empty();
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String uuidStr = pdc.get(keyToolUuid, PersistentDataType.STRING);
        if (uuidStr == null || uuidStr.trim().isEmpty()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(uuidStr));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<UUID> getOwnerUuid(ItemStack itemStack) {
        if (!isManagedTool(itemStack)) return Optional.empty();
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String uuidStr = pdc.get(keyOwnerUuid, PersistentDataType.STRING);
        if (uuidStr == null || uuidStr.trim().isEmpty()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(uuidStr));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<ToolType> getToolType(ItemStack itemStack) {
        if (!isManagedTool(itemStack)) return Optional.empty();
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String typeStr = pdc.get(keyToolType, PersistentDataType.STRING);
        return Optional.ofNullable(ToolType.fromString(typeStr));
    }

    public Optional<ToolMaterial> getMaterial(ItemStack itemStack) {
        if (!isManagedTool(itemStack)) return Optional.empty();
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String matStr = pdc.get(keyMaterial, PersistentDataType.STRING);
        if (matStr != null) {
            return Optional.of(ToolMaterial.fromString(matStr));
        }
        return Optional.empty();
    }

    public void stampMetadata(ItemStack itemStack, ToolProgression progression, int maxDurability) {
        stampMetadata(itemStack, progression, maxDurability, 100, 1.0);
    }

    public void stampMetadata(ItemStack itemStack, ToolProgression progression, int maxDurability, int requiredXp) {
        stampMetadata(itemStack, progression, maxDurability, requiredXp, 1.0);
    }

    public void stampMetadata(ItemStack itemStack, ToolProgression progression, int maxDurability, int requiredXp, double miningSpeed) {
        if (itemStack == null || progression == null) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyToolUuid, PersistentDataType.STRING, progression.getToolUuid().toString());
        pdc.set(keyOwnerUuid, PersistentDataType.STRING, progression.getOwnerUuid().toString());
        pdc.set(keyToolType, PersistentDataType.STRING, progression.getToolType().name());
        pdc.set(keyMaterial, PersistentDataType.STRING, progression.getMaterial().name());
        pdc.set(keySchemaVersion, PersistentDataType.INTEGER, progression.getSchemaVersion());
        pdc.set(keyOverflowXp, PersistentDataType.INTEGER, progression.getOverflowXp());

        int customModelData = configManager != null ? configManager.getCustomModelData(progression.getMaterial()) : 0;
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        } else {
            meta.setCustomModelData(null);
        }

        String matDisplayName = progression.getMaterial().getDisplayName();
        String progressBar = buildProgressBar(progression.getXp(), requiredXp);

        // Display Name
        String displayNameTmpl;
        if (progression.getState() == ToolState.DESTROYED) {
            displayNameTmpl = configManager != null ? configManager.getBrokenDisplayNameTemplate() : "&cBroken %material% Pickaxe";
        } else if (progression.getState() == ToolState.LOST) {
            displayNameTmpl = configManager != null ? configManager.getLostDisplayNameTemplate() : "&e%material% Pickaxe (Lost)";
        } else {
            displayNameTmpl = configManager != null ? configManager.getDisplayNameTemplate() : "&fStable %material% Pickaxe &8(Lvl %level%)";
        }
        meta.setDisplayName(formatLine(displayNameTmpl, progression, maxDurability, requiredXp, miningSpeed, progressBar));

        // Lore
        List<String> rawLoreList = configManager != null ? configManager.getLoreFormatList() : null;
        if (rawLoreList == null || rawLoreList.isEmpty()) {
            rawLoreList = new ArrayList<>();
            rawLoreList.add("");
            rawLoreList.add("&7Mining Speed: &a%speed%");
            rawLoreList.add("&7Durability: &f%current%/%max%");
            rawLoreList.add("");
            rawLoreList.add("&7Progress to Level %next_level%: &a%percentage%%");
            rawLoreList.add("%progress_bar% &a%xp%&7/&a%max_xp% &7XP");
        }

        int maxLineLength = configManager != null ? configManager.getLoreMaxLineLength() : 35;

        int maxLvl = configManager != null ? configManager.getMaxLevel(progression.getMaterial()) : 3;
        boolean isMaxLevelOfTier = progression.getLevel() >= maxLvl;
        boolean hasNextTier = configManager != null && configManager.getNextTier(progression.getMaterial()) != null;
        // Only treat as truly max (hide progress) when there is no next tier to upgrade to
        boolean isTrulyMax = isMaxLevelOfTier && !hasNextTier;

        List<String> lore = new ArrayList<>();
        for (String rawLine : rawLoreList) {
            // Only skip progress lines when truly max (no next tier available)
            if (isTrulyMax && (rawLine.contains("%next_level%") || rawLine.contains("%percentage%") || rawLine.contains("%next_tier%"))) {
                continue;
            }
            if (isTrulyMax && rawLine.contains("%progress_bar%")) {
                continue;
            }

            String lineToFormat = rawLine;
            if (isMaxLevelOfTier && hasNextTier) {
                if (lineToFormat.contains("Progress to Level %next_level%")) {
                    lineToFormat = lineToFormat.replace("Progress to Level %next_level%", "Progress to Tier %next_tier%");
                } else if (lineToFormat.contains("progress to level %next_level%")) {
                    lineToFormat = lineToFormat.replace("progress to level %next_level%", "progress to tier %next_tier%");
                } else if (lineToFormat.contains("Progress to Level")) {
                    lineToFormat = lineToFormat.replace("Progress to Level", "Progress to Tier");
                }
            }

            String formatted = formatLine(lineToFormat, progression, maxDurability, requiredXp, miningSpeed, progressBar);
            if (rawLine.contains("%progress_bar%")) {
                lore.add(formatted);
            } else {
                List<String> wrapped = io.github.kaivian.kupdater.core.util.TextWrapUtils.wrapText(formatted, maxLineLength);
                lore.addAll(wrapped);
            }
        }

        if (isTrulyMax) {
            // No next tier — truly max (e.g. Netherite Lv5)
            String maxReachedText = configManager != null ? configManager.getMaxLevelReachedText() : "&a&l✦ MAX LEVEL REACHED ✦";
            lore.add(ChatColor.translateAlternateColorCodes('&', maxReachedText));
            if (progression.getOverflowXp() > 0) {
                String storedXpTmpl = configManager != null ? configManager.getStoredXpText() : "&7Stored XP: &e%stored_xp%";
                String storedXpLine = storedXpTmpl
                        .replace("%stored_xp%", String.format("%,d", progression.getOverflowXp()))
                        .replace("%overflow_xp%", String.format("%,d", progression.getOverflowXp()))
                        .replace("%xp_pool%", String.format("%,d", progression.getOverflowXp()));
                lore.add(ChatColor.translateAlternateColorCodes('&', storedXpLine));
            }
        } else if (isMaxLevelOfTier && hasNextTier) {
            // Max level of current tier but can upgrade to next tier
            // Show overflow XP pool if present
            if (progression.getOverflowXp() > 0) {
                String storedXpTmpl = configManager != null ? configManager.getStoredXpText() : "&7Stored XP: &e%stored_xp%";
                String storedXpLine = storedXpTmpl
                        .replace("%stored_xp%", String.format("%,d", progression.getOverflowXp()))
                        .replace("%overflow_xp%", String.format("%,d", progression.getOverflowXp()))
                        .replace("%xp_pool%", String.format("%,d", progression.getOverflowXp()));
                lore.add(ChatColor.translateAlternateColorCodes('&', storedXpLine));
            }
            // Show upgrade guide when XP is sufficient
            if (progression.getXp() >= requiredXp) {
                String guideStr = buildMaxLevelGuide(progression, maxLvl, requiredXp);
                if (guideStr != null && !guideStr.isEmpty()) {
                    lore.add("");
                    List<String> wrappedGuide = io.github.kaivian.kupdater.core.util.TextWrapUtils.wrapText(guideStr, maxLineLength);
                    lore.addAll(wrappedGuide);
                }
            }
        }

        meta.setLore(lore);

        if (meta instanceof org.bukkit.inventory.meta.Damageable) {
            org.bukkit.inventory.meta.Damageable dmgMeta = (org.bukkit.inventory.meta.Damageable) meta;
            int currentDur = progression.getCurrentDurability();

            boolean setCustomMax = false;
            try {
                java.lang.reflect.Method setMaxDamageMethod = meta.getClass().getMethod("setMaxDamage", Integer.class);
                setMaxDamageMethod.invoke(meta, maxDurability);
                setCustomMax = true;
            } catch (Throwable t1) {
                try {
                    java.lang.reflect.Method setMaxDamageMethod = meta.getClass().getMethod("setMaxDamage", int.class);
                    setMaxDamageMethod.invoke(meta, maxDurability);
                    setCustomMax = true;
                } catch (Throwable t2) {
                }
            }

            if (setCustomMax) {
                if (progression.getState() == ToolState.DESTROYED || currentDur <= 0) {
                    dmgMeta.setDamage(maxDurability - 1);
                } else {
                    int damage = Math.max(0, maxDurability - currentDur);
                    dmgMeta.setDamage(Math.min(maxDurability - 1, damage));
                }
            } else {
                short maxVanillaDurability = itemStack.getType().getMaxDurability();
                if (maxVanillaDurability > 0) {
                    if (progression.getState() == ToolState.DESTROYED || currentDur <= 0) {
                        dmgMeta.setDamage(maxVanillaDurability - 1);
                    } else {
                        double durRatio = Math.min(1.0, Math.max(0.0, (double) currentDur / maxDurability));
                        int visualDamage = (int) Math.round((1.0 - durRatio) * (maxVanillaDurability - 1));
                        visualDamage = Math.min(maxVanillaDurability - 1, Math.max(0, visualDamage));
                        dmgMeta.setDamage(visualDamage);
                    }
                }
            }
        }

        // Preserve default attack attributes (attack damage, attack speed) when setting custom meta
        try {
            if (!meta.hasAttributeModifiers() && itemStack.getType() != null) {
                com.google.common.collect.Multimap<org.bukkit.attribute.Attribute, org.bukkit.attribute.AttributeModifier> defaultMods =
                        itemStack.getType().getDefaultAttributeModifiers(org.bukkit.inventory.EquipmentSlot.HAND);
                if (defaultMods != null && !defaultMods.isEmpty()) {
                    for (java.util.Map.Entry<org.bukkit.attribute.Attribute, org.bukkit.attribute.AttributeModifier> entry : defaultMods.entries()) {
                        meta.addAttributeModifier(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        itemStack.setItemMeta(meta);
    }


    private String buildProgressBar(int currentXp, int requiredXp) {
        String symbol = configManager != null ? configManager.getProgressBarSymbol() : " ";
        int length = configManager != null ? configManager.getProgressBarLength() : 22;
        String completedColor = configManager != null ? configManager.getProgressBarCompletedColor() : "&a&l&m";
        String remainingColor = configManager != null ? configManager.getProgressBarRemainingColor() : "&8&l&m";

        if (requiredXp <= 0) requiredXp = 1;
        double ratio = Math.min(1.0, Math.max(0.0, (double) currentXp / requiredXp));
        int filled = (int) Math.round(ratio * length);
        filled = Math.min(length, Math.max(0, filled));

        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.translateAlternateColorCodes('&', completedColor));
        for (int i = 0; i < filled; i++) {
            sb.append(symbol);
        }
        sb.append(ChatColor.translateAlternateColorCodes('&', remainingColor));
        for (int i = 0; i < length - filled; i++) {
            sb.append(symbol);
        }
        return sb.toString();
    }

    private String formatLine(String line, ToolProgression progression, int maxDurability, int requiredXp, double miningSpeed, String progressBar) {
        if (line == null) return "";
        String matDisplayName = progression.getMaterial().getDisplayName();
        ToolMaterial nextTierMat = configManager != null ? configManager.getNextTier(progression.getMaterial()) : null;
        String nextTierDisplayName = nextTierMat != null ? nextTierMat.getDisplayName() : "";

        double percent = requiredXp > 0 ? Math.min(100.0, Math.max(0.0, ((double) progression.getXp() / requiredXp) * 100.0)) : 0.0;

        String formattedXp = String.format("%,d", progression.getXp());
        String formattedReqXp = String.format("%,d", requiredXp);
        String formattedSpeed = String.format("%.2f", miningSpeed);
        String formattedOverflowXp = String.format("%,d", progression.getOverflowXp());

        int nextLvl = progression.getLevel() + 1;
        int maxLvl = configManager != null ? configManager.getMaxLevel(progression.getMaterial()) : 3;
        if (nextLvl > maxLvl) nextLvl = maxLvl;

        String stateStr = "";
        if (progression.getState() == ToolState.ACTIVE) stateStr = "";
        else if (progression.getState() == ToolState.DESTROYED) stateStr = "Broken";
        else if (progression.getState() == ToolState.LOST) stateStr = "Lost";

        String result = line
                .replace("%material%", matDisplayName)
                .replace("%tier%", matDisplayName)
                .replace("%next_tier%", nextTierDisplayName)
                .replace("%level%", String.valueOf(progression.getLevel()))
                .replace("%next_level%", String.valueOf(nextLvl))
                .replace("%xp%", String.valueOf(progression.getXp()))
                .replace("%current_xp%", String.valueOf(progression.getXp()))
                .replace("%max_xp%", String.valueOf(requiredXp))
                .replace("%required_xp%", String.valueOf(requiredXp))
                .replace("%xp_formatted%", formattedXp)
                .replace("%max_xp_formatted%", formattedReqXp)
                .replace("%required_xp_formatted%", formattedReqXp)
                .replace("%overflow_xp%", String.valueOf(progression.getOverflowXp()))
                .replace("%xp_pool%", String.valueOf(progression.getOverflowXp()))
                .replace("%stored_xp%", String.valueOf(progression.getOverflowXp()))
                .replace("%overflow_xp_formatted%", formattedOverflowXp)
                .replace("%percentage%", String.format("%.0f", percent))
                .replace("%progress_percent%", String.format("%.0f", percent))
                .replace("%progress_bar%", progressBar)
                .replace("%speed%", formattedSpeed)
                .replace("%mining_speed%", formattedSpeed)
                .replace("%current%", String.valueOf(progression.getCurrentDurability()))
                .replace("%current_durability%", String.valueOf(progression.getCurrentDurability()))
                .replace("%durability%", String.valueOf(progression.getCurrentDurability()))
                .replace("%max%", String.valueOf(maxDurability))
                .replace("%max_durability%", String.valueOf(maxDurability))
                .replace("%state%", stateStr);

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String buildMaxLevelGuide(ToolProgression progression, int maxLvl, int requiredXp) {
        if (configManager == null) return "";
        if (progression.getLevel() < maxLvl || progression.getXp() < requiredXp) return "";

        ToolMaterial mat = progression.getMaterial();
        ToolMaterial nextTier = configManager.getNextTier(mat);
        if (nextTier == null) return "";

        if (mat == ToolMaterial.DIAMOND || nextTier == ToolMaterial.NETHERITE) {
            String tmpl = configManager.getMaxLevelGuideSmithing();
            return tmpl
                    .replace("%upgrade_amount%", "1")
                    .replace("%upgrade_item%", "Netherite Ingot")
                    .replace("%upgrade_target%", nextTier.getDisplayName());
        }

        UpgradeRecipe recipe = configManager.getUpgradeRecipe(mat);
        if (recipe == null) return "";

        String tmpl = configManager.getMaxLevelGuideCrafting();
        return tmpl
                .replace("%upgrade_amount%", String.valueOf(recipe.getAmount()))
                .replace("%upgrade_item%", formatItemName(recipe.getMaterial()))
                .replace("%upgrade_target%", nextTier.getDisplayName());
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
