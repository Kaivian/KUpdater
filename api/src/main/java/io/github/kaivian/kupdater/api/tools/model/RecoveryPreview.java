package io.github.kaivian.kupdater.api.tools.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable server-generated source of truth representing the availability, costs,
 * penalties, and calculated result for a pickaxe recovery.
 */
public final class RecoveryPreview {

    public enum Status {
        AVAILABLE,
        ON_COOLDOWN,
        INSUFFICIENT_FUNDS,
        MISSING_ITEMS,
        INVALID_TOOL_STATE,
        ECONOMY_UNAVAILABLE,
        INVENTORY_FULL,
        TRANSACTION_LOCKED,
        MAX_RECOVERIES_EXCEEDED,
        DISABLED
    }

    private final Status status;
    private final UUID ownerUuid;
    private final ToolProgression progression;
    private final ToolRecoveryState recoveryState;
    private final int recoveryNumber; // 1-indexed (recoveryCount + 1)
    private final int penaltyLevel;
    private final double durabilityPenaltyPercent;
    private final int resultingDurability;
    private final int maxDurability;
    private final double currencyCost;
    private final double currencyAvailable;
    private final boolean currencySufficient;
    private final List<ItemStack> requiredItems;
    private final boolean hasRequiredItems;
    private final long cooldownDurationSeconds;
    private final long cooldownRemainingSeconds;
    private final String failureReason;

    public RecoveryPreview(Status status, UUID ownerUuid, ToolProgression progression,
                           ToolRecoveryState recoveryState, int recoveryNumber, int penaltyLevel,
                           double durabilityPenaltyPercent, int resultingDurability, int maxDurability,
                           double currencyCost, double currencyAvailable, boolean currencySufficient,
                           List<ItemStack> requiredItems, boolean hasRequiredItems,
                           long cooldownDurationSeconds, long cooldownRemainingSeconds,
                           String failureReason) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.ownerUuid = ownerUuid;
        this.progression = progression;
        this.recoveryState = recoveryState;
        this.recoveryNumber = recoveryNumber;
        this.penaltyLevel = penaltyLevel;
        this.durabilityPenaltyPercent = durabilityPenaltyPercent;
        this.resultingDurability = resultingDurability;
        this.maxDurability = maxDurability;
        this.currencyCost = Math.max(0.0, currencyCost);
        this.currencyAvailable = Math.max(0.0, currencyAvailable);
        this.currencySufficient = currencySufficient;
        this.requiredItems = requiredItems != null ? Collections.unmodifiableList(new ArrayList<>(requiredItems)) : Collections.emptyList();
        this.hasRequiredItems = hasRequiredItems;
        this.cooldownDurationSeconds = Math.max(0, cooldownDurationSeconds);
        this.cooldownRemainingSeconds = Math.max(0, cooldownRemainingSeconds);
        this.failureReason = failureReason != null ? failureReason : "";
    }

    public Status getStatus() {
        return status;
    }

    public boolean isAvailable() {
        return status == Status.AVAILABLE;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public ToolProgression getProgression() {
        return progression;
    }

    public ToolRecoveryState getRecoveryState() {
        return recoveryState;
    }

    public int getRecoveryNumber() {
        return recoveryNumber;
    }

    public int getPenaltyLevel() {
        return penaltyLevel;
    }

    public double getDurabilityPenaltyPercent() {
        return durabilityPenaltyPercent;
    }

    public int getResultingDurability() {
        return resultingDurability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public double getCurrencyCost() {
        return currencyCost;
    }

    public double getCurrencyAvailable() {
        return currencyAvailable;
    }

    public boolean isCurrencySufficient() {
        return currencySufficient;
    }

    public List<ItemStack> getRequiredItems() {
        return requiredItems;
    }

    public boolean hasRequiredItems() {
        return hasRequiredItems;
    }

    public long getCooldownDurationSeconds() {
        return cooldownDurationSeconds;
    }

    public long getCooldownRemainingSeconds() {
        return cooldownRemainingSeconds;
    }

    public boolean isOnCooldown() {
        return cooldownRemainingSeconds > 0;
    }

    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public String toString() {
        return "RecoveryPreview{" +
                "status=" + status +
                ", recoveryNumber=" + recoveryNumber +
                ", penaltyLevel=" + penaltyLevel +
                ", durabilityPenaltyPercent=" + durabilityPenaltyPercent +
                ", currencyCost=" + currencyCost +
                ", failureReason='" + failureReason + '\'' +
                '}';
    }
}
