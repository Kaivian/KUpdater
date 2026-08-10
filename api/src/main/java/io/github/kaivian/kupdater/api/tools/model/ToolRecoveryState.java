package io.github.kaivian.kupdater.api.tools.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain model representing persistent pickaxe recovery tracking state.
 */
public final class ToolRecoveryState {

    private final UUID toolUuid;
    private final UUID ownerUuid;
    private final int recoveryCount;
    private final Instant lastRecoveryAt;
    private final Instant nextRecoveryAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ToolRecoveryState(UUID toolUuid, UUID ownerUuid, int recoveryCount,
                             Instant lastRecoveryAt, Instant nextRecoveryAt,
                             Instant createdAt, Instant updatedAt) {
        this.toolUuid = Objects.requireNonNull(toolUuid, "toolUuid cannot be null");
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid cannot be null");
        this.recoveryCount = Math.max(0, recoveryCount);
        this.lastRecoveryAt = lastRecoveryAt;
        this.nextRecoveryAt = nextRecoveryAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static ToolRecoveryState initial(UUID toolUuid, UUID ownerUuid) {
        Instant now = Instant.now();
        return new ToolRecoveryState(toolUuid, ownerUuid, 0, null, null, now, now);
    }

    public UUID getToolUuid() {
        return toolUuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public int getRecoveryCount() {
        return recoveryCount;
    }

    public Instant getLastRecoveryAt() {
        return lastRecoveryAt;
    }

    public Instant getNextRecoveryAt() {
        return nextRecoveryAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOnCooldown(Instant now) {
        if (nextRecoveryAt == null) return false;
        return now.isBefore(nextRecoveryAt);
    }

    public long getRemainingCooldownSeconds(Instant now) {
        if (!isOnCooldown(now)) return 0;
        return Math.max(0, nextRecoveryAt.getEpochSecond() - now.getEpochSecond());
    }

    public ToolRecoveryState withToolUuid(UUID newToolUuid) {
        return new ToolRecoveryState(
                Objects.requireNonNull(newToolUuid, "newToolUuid cannot be null"),
                ownerUuid,
                recoveryCount,
                lastRecoveryAt,
                nextRecoveryAt,
                createdAt,
                Instant.now()
        );
    }

    public ToolRecoveryState withCompletedRecovery(Instant recoveryTime, Instant cooldownExpiresAt) {
        return new ToolRecoveryState(
                toolUuid,
                ownerUuid,
                recoveryCount + 1,
                recoveryTime,
                cooldownExpiresAt,
                createdAt,
                Instant.now()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolRecoveryState that = (ToolRecoveryState) o;
        return recoveryCount == that.recoveryCount &&
                Objects.equals(toolUuid, that.toolUuid) &&
                Objects.equals(ownerUuid, that.ownerUuid) &&
                Objects.equals(lastRecoveryAt, that.lastRecoveryAt) &&
                Objects.equals(nextRecoveryAt, that.nextRecoveryAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolUuid, ownerUuid, recoveryCount, lastRecoveryAt, nextRecoveryAt);
    }

    @Override
    public String toString() {
        return "ToolRecoveryState{" +
                "toolUuid=" + toolUuid +
                ", ownerUuid=" + ownerUuid +
                ", recoveryCount=" + recoveryCount +
                ", lastRecoveryAt=" + lastRecoveryAt +
                ", nextRecoveryAt=" + nextRecoveryAt +
                '}';
    }
}
