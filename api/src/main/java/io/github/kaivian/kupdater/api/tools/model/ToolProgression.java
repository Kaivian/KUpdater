package io.github.kaivian.kupdater.api.tools.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Authoritative domain model representing a player's tool progression.
 */
public final class ToolProgression {

    private final UUID toolUuid;
    private final UUID ownerUuid;
    private final ToolType toolType;
    private final ToolMaterial material;
    private final int level;
    private final int xp;
    private final int overflowXp;
    private final ToolState state;
    private final int currentDurability;
    private final int schemaVersion;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ToolProgression(UUID toolUuid, UUID ownerUuid, ToolType toolType, int level, ToolState state,
                           int currentDurability, int schemaVersion, Instant createdAt, Instant updatedAt) {
        this(toolUuid, ownerUuid, toolType, ToolMaterial.WOODEN, level, 0, 0, state, currentDurability, schemaVersion, createdAt, updatedAt);
    }

    public ToolProgression(UUID toolUuid, UUID ownerUuid, ToolType toolType, ToolMaterial material, int level, ToolState state,
                           int currentDurability, int schemaVersion, Instant createdAt, Instant updatedAt) {
        this(toolUuid, ownerUuid, toolType, material, level, 0, 0, state, currentDurability, schemaVersion, createdAt, updatedAt);
    }

    public ToolProgression(UUID toolUuid, UUID ownerUuid, ToolType toolType, ToolMaterial material, int level, int xp, ToolState state,
                           int currentDurability, int schemaVersion, Instant createdAt, Instant updatedAt) {
        this(toolUuid, ownerUuid, toolType, material, level, xp, 0, state, currentDurability, schemaVersion, createdAt, updatedAt);
    }

    public ToolProgression(UUID toolUuid, UUID ownerUuid, ToolType toolType, ToolMaterial material, int level, int xp, int overflowXp, ToolState state,
                           int currentDurability, int schemaVersion, Instant createdAt, Instant updatedAt) {
        this.toolUuid = Objects.requireNonNull(toolUuid, "toolUuid cannot be null");
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid cannot be null");
        this.toolType = Objects.requireNonNull(toolType, "toolType cannot be null");
        this.material = material != null ? material : ToolMaterial.WOODEN;
        this.level = Math.max(1, level);
        this.xp = Math.max(0, xp);
        this.overflowXp = Math.max(0, overflowXp);
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.currentDurability = Math.max(0, currentDurability);
        this.schemaVersion = schemaVersion <= 0 ? 1 : schemaVersion;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getToolUuid() {
        return toolUuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public ToolType getToolType() {
        return toolType;
    }

    public ToolMaterial getMaterial() {
        return material;
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getOverflowXp() {
        return overflowXp;
    }

    public ToolState getState() {
        return state;
    }

    public int getCurrentDurability() {
        return currentDurability;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public ToolProgression withLevel(int newLevel) {
        return new ToolProgression(toolUuid, ownerUuid, toolType, material, newLevel, xp, overflowXp, state, currentDurability, schemaVersion, createdAt, Instant.now());
    }

    public ToolProgression withXp(int newXp) {
        return new ToolProgression(toolUuid, ownerUuid, toolType, material, level, newXp, overflowXp, state, currentDurability, schemaVersion, createdAt, Instant.now());
    }

    public ToolProgression withOverflowXp(int newOverflowXp) {
        return new ToolProgression(toolUuid, ownerUuid, toolType, material, level, xp, newOverflowXp, state, currentDurability, schemaVersion, createdAt, Instant.now());
    }

    public ToolProgression withMaterialAndLevel(ToolMaterial newMaterial, int newLevel, int newDurability) {
        return new ToolProgression(toolUuid, ownerUuid, toolType, newMaterial, newLevel, 0, overflowXp, state, newDurability, schemaVersion, createdAt, Instant.now());
    }

    public ToolProgression withState(ToolState newState) {
        return new ToolProgression(toolUuid, ownerUuid, toolType, material, level, xp, overflowXp, newState, currentDurability, schemaVersion, createdAt, Instant.now());
    }

    public ToolProgression withDurability(int newDurability) {
        return new ToolProgression(toolUuid, ownerUuid, toolType, material, level, xp, overflowXp, state, newDurability, schemaVersion, createdAt, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolProgression that = (ToolProgression) o;
        return level == that.level &&
                xp == that.xp &&
                overflowXp == that.overflowXp &&
                currentDurability == that.currentDurability &&
                schemaVersion == that.schemaVersion &&
                Objects.equals(toolUuid, that.toolUuid) &&
                Objects.equals(ownerUuid, that.ownerUuid) &&
                toolType == that.toolType &&
                material == that.material &&
                state == that.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolUuid, ownerUuid, toolType, material, level, xp, overflowXp, state, currentDurability, schemaVersion);
    }

    @Override
    public String toString() {
        return "ToolProgression{" +
                "toolUuid=" + toolUuid +
                ", ownerUuid=" + ownerUuid +
                ", toolType=" + toolType +
                ", material=" + material +
                ", level=" + level +
                ", xp=" + xp +
                ", overflowXp=" + overflowXp +
                ", state=" + state +
                ", currentDurability=" + currentDurability +
                ", schemaVersion=" + schemaVersion +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
