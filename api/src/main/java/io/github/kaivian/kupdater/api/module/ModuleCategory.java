package io.github.kaivian.kupdater.api.module;

/**
 * Categorizes KUpdater gameplay enhancement modules.
 */
public enum ModuleCategory {
    TOOLS("Tools & Equipment Progression"),
    COMBAT("Combat Mechanics"),
    FARMING("Farming & Agriculture"),
    MINING("Mining Progression & QoL"),
    ECONOMY("Economy Enhancements"),
    EXPLORATION("Exploration & World"),
    QOL("Quality of Life"),
    PROGRESSION("Progression & Achievements"),
    OTHER("Miscellaneous Enhancements");

    private final String displayName;

    ModuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
