package io.github.kaivian.kupdater.api.tools.model;

import java.util.Objects;

/**
 * Abstraction representing penalties to apply during future tool recovery.
 */
public final class ToolRecoveryPenalty {

    public enum Type {
        LEVEL_PENALTY,
        STAT_PENALTY,
        DURABILITY_PENALTY,
        PERCENTAGE_PENALTY,
        CUSTOM
    }

    private final Type type;
    private final double value;
    private final String description;

    public ToolRecoveryPenalty(Type type, double value, String description) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.value = value;
        this.description = description != null ? description : "";
    }

    public Type getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolRecoveryPenalty that = (ToolRecoveryPenalty) o;
        return Double.compare(that.value, value) == 0 && type == that.type && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, description);
    }

    @Override
    public String toString() {
        return "ToolRecoveryPenalty{" +
                "type=" + type +
                ", value=" + value +
                ", description='" + description + '\'' +
                '}';
    }
}
