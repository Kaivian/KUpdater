package io.github.kaivian.kupdater.features.tools.common.validation;

import io.github.kaivian.kupdater.api.tools.model.ToolStat;
import io.github.kaivian.kupdater.api.tools.model.ToolType;

import java.util.logging.Logger;

/**
 * Validates tool progression stat bounds and prevents game-breaking configurations.
 */
public class ProgressionBalanceValidator {

    private static final double MAX_ALLOWED_MINING_SPEED = 100.0;
    private static final int MAX_ALLOWED_DURABILITY = 1_000_000;

    private final Logger logger;

    public ProgressionBalanceValidator(Logger logger) {
        this.logger = logger != null ? logger : Logger.getLogger("ProgressionBalanceValidator");
    }

    /**
     * Validates if a target stat configuration is balanced and safe.
     *
     * @param toolType Tool type
     * @param targetLevel Target progression level
     * @param targetStat Target stat configuration
     * @return true if valid and balanced, false otherwise
     */
    public boolean validateStatBalance(ToolType toolType, int targetLevel, ToolStat targetStat) {
        if (targetStat == null) return false;

        if (targetStat.getMiningSpeedMultiplier() <= 0 || targetStat.getMiningSpeedMultiplier() > MAX_ALLOWED_MINING_SPEED) {
            logger.warning("[BalanceValidator] Rejected level " + targetLevel + " for " + toolType
                    + ": mining speed " + targetStat.getMiningSpeedMultiplier() + " out of safe bounds (0 - " + MAX_ALLOWED_MINING_SPEED + ")");
            return false;
        }

        if (targetStat.getMaxDurability() <= 0 || targetStat.getMaxDurability() > MAX_ALLOWED_DURABILITY) {
            logger.warning("[BalanceValidator] Rejected level " + targetLevel + " for " + toolType
                    + ": durability " + targetStat.getMaxDurability() + " out of safe bounds (1 - " + MAX_ALLOWED_DURABILITY + ")");
            return false;
        }

        return true;
    }
}
