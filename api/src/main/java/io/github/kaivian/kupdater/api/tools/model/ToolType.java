package io.github.kaivian.kupdater.api.tools.model;

/**
 * Supported tool categories within KUpdater tool progression framework.
 */
public enum ToolType {
    PICKAXE("Pickaxe"),
    AXE("Axe"),
    SHOVEL("Shovel"),
    HOE("Hoe"),
    SWORD("Sword");

    private final String displayName;

    ToolType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ToolType fromString(String str) {
        if (str == null) return null;
        for (ToolType type : values()) {
            if (type.name().equalsIgnoreCase(str) || type.displayName.equalsIgnoreCase(str)) {
                return type;
            }
        }
        return null;
    }
}
