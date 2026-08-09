package io.github.kaivian.kupdater.api.tools.model;

import java.util.Objects;

/**
 * Value object representing intrinsic stats for a specific tool level.
 */
public final class ToolStat {

    private final double miningSpeedMultiplier;
    private final int maxDurability;
    private final int requiredXp;

    public ToolStat(double miningSpeedMultiplier, int maxDurability) {
        this(miningSpeedMultiplier, maxDurability, 100);
    }

    public ToolStat(double miningSpeedMultiplier, int maxDurability, int requiredXp) {
        this.miningSpeedMultiplier = Math.max(0.1, miningSpeedMultiplier);
        this.maxDurability = Math.max(1, maxDurability);
        this.requiredXp = Math.max(1, requiredXp);
    }

    public double getMiningSpeedMultiplier() {
        return miningSpeedMultiplier;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getRequiredXp() {
        return requiredXp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolStat toolStat = (ToolStat) o;
        return Double.compare(toolStat.miningSpeedMultiplier, miningSpeedMultiplier) == 0 &&
                maxDurability == toolStat.maxDurability &&
                requiredXp == toolStat.requiredXp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(miningSpeedMultiplier, maxDurability, requiredXp);
    }

    @Override
    public String toString() {
        return "ToolStat{" +
                "miningSpeedMultiplier=" + miningSpeedMultiplier +
                ", maxDurability=" + maxDurability +
                ", requiredXp=" + requiredXp +
                '}';
    }
}
