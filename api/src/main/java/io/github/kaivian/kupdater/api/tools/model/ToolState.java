package io.github.kaivian.kupdater.api.tools.model;

/**
 * Represents the lifecycle state of a managed KUpdater tool progression.
 */
public enum ToolState {
    /**
     * Active tool bound to owner, fully functional for gameplay actions.
     */
    ACTIVE,

    /**
     * Tool is lost/inaccessible to owner. Progression remains persisted in database.
     */
    LOST,

    /**
     * Tool durability exhausted. Physical item remains unusable in inventory/world.
     * Progression remains persisted in database.
     */
    DESTROYED;

    public static ToolState fromString(String str) {
        if (str == null) return null;
        for (ToolState state : values()) {
            if (state.name().equalsIgnoreCase(str)) {
                return state;
            }
        }
        return null;
    }
}
