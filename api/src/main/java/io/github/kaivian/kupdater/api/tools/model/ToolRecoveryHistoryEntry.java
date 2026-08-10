package io.github.kaivian.kupdater.api.tools.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit log record representing a completed tool recovery transaction.
 */
public final class ToolRecoveryHistoryEntry {

    private final long id;
    private final UUID toolUuid;
    private final UUID ownerUuid;
    private final int recoveryCount;
    private final int penaltyLevel;
    private final double currencyCost;
    private final String itemsConsumedJson;
    private final double durabilityPenaltyPercent;
    private final Instant recoveredAt;

    public ToolRecoveryHistoryEntry(long id, UUID toolUuid, UUID ownerUuid, int recoveryCount,
                                    int penaltyLevel, double currencyCost, String itemsConsumedJson,
                                    double durabilityPenaltyPercent, Instant recoveredAt) {
        this.id = id;
        this.toolUuid = Objects.requireNonNull(toolUuid, "toolUuid cannot be null");
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid cannot be null");
        this.recoveryCount = recoveryCount;
        this.penaltyLevel = penaltyLevel;
        this.currencyCost = currencyCost;
        this.itemsConsumedJson = itemsConsumedJson != null ? itemsConsumedJson : "[]";
        this.durabilityPenaltyPercent = durabilityPenaltyPercent;
        this.recoveredAt = recoveredAt != null ? recoveredAt : Instant.now();
    }

    public static ToolRecoveryHistoryEntry create(UUID toolUuid, UUID ownerUuid, int recoveryCount,
                                                 int penaltyLevel, double currencyCost,
                                                 String itemsConsumedJson, double durabilityPenaltyPercent) {
        return new ToolRecoveryHistoryEntry(0L, toolUuid, ownerUuid, recoveryCount, penaltyLevel,
                currencyCost, itemsConsumedJson, durabilityPenaltyPercent, Instant.now());
    }

    public long getId() {
        return id;
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

    public int getPenaltyLevel() {
        return penaltyLevel;
    }

    public double getCurrencyCost() {
        return currencyCost;
    }

    public String getItemsConsumedJson() {
        return itemsConsumedJson;
    }

    public double getDurabilityPenaltyPercent() {
        return durabilityPenaltyPercent;
    }

    public Instant getRecoveredAt() {
        return recoveredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolRecoveryHistoryEntry that = (ToolRecoveryHistoryEntry) o;
        return id == that.id && recoveryCount == that.recoveryCount &&
                penaltyLevel == that.penaltyLevel && Double.compare(that.currencyCost, currencyCost) == 0 &&
                Double.compare(that.durabilityPenaltyPercent, durabilityPenaltyPercent) == 0 &&
                Objects.equals(toolUuid, that.toolUuid) && Objects.equals(ownerUuid, that.ownerUuid) &&
                Objects.equals(recoveredAt, that.recoveredAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, toolUuid, ownerUuid, recoveryCount, penaltyLevel, currencyCost, durabilityPenaltyPercent, recoveredAt);
    }

    @Override
    public String toString() {
        return "ToolRecoveryHistoryEntry{" +
                "id=" + id +
                ", toolUuid=" + toolUuid +
                ", ownerUuid=" + ownerUuid +
                ", recoveryCount=" + recoveryCount +
                ", penaltyLevel=" + penaltyLevel +
                ", currencyCost=" + currencyCost +
                ", durabilityPenaltyPercent=" + durabilityPenaltyPercent +
                ", recoveredAt=" + recoveredAt +
                '}';
    }
}
