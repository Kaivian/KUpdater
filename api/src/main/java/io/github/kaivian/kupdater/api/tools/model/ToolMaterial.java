package io.github.kaivian.kupdater.api.tools.model;

import org.bukkit.Material;

/**
 * Enum representing tool material tiers for Pickaxes.
 */
public enum ToolMaterial {
    WOODEN("Wooden", Material.WOODEN_PICKAXE),
    STONE("Stone", Material.STONE_PICKAXE),
    COPPER("Copper", Material.STONE_PICKAXE),
    IRON("Iron", Material.IRON_PICKAXE),
    GOLD("Golden", Material.GOLDEN_PICKAXE),
    DIAMOND("Diamond", Material.DIAMOND_PICKAXE),
    NETHERITE("Netherite", Material.NETHERITE_PICKAXE);

    private final String displayName;
    private final Material bukkitMaterial;

    ToolMaterial(String displayName, Material bukkitMaterial) {
        this.displayName = displayName;
        this.bukkitMaterial = bukkitMaterial;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getBukkitMaterial() {
        if (this == COPPER) {
            Material copperMat = Material.matchMaterial("COPPER_PICKAXE");
            if (copperMat != null) return copperMat;
        }
        return bukkitMaterial;
    }

    public static ToolMaterial fromString(String name) {
        if (name == null) return WOODEN;
        try {
            return ToolMaterial.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Check material name matching (e.g. WOODEN_PICKAXE)
            for (ToolMaterial mat : values()) {
                if (mat.name().equalsIgnoreCase(name) || mat.getBukkitMaterial().name().equalsIgnoreCase(name)) {
                    return mat;
                }
            }
            return WOODEN;
        }
    }

    public static ToolMaterial fromBukkitMaterial(Material material) {
        if (material == null) return WOODEN;
        if (material == Material.STONE_PICKAXE) return STONE;
        for (ToolMaterial mat : values()) {
            if (mat.getBukkitMaterial() == material) {
                return mat;
            }
        }
        return WOODEN;
    }
}
