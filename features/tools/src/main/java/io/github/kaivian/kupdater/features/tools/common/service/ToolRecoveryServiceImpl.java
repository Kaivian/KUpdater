package io.github.kaivian.kupdater.features.tools.common.service;

import io.github.kaivian.kupdater.api.tools.model.RecoveryPreview;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryPenalty;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryState;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRecoveryRepository;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolRecoveryService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Primary implementation of ToolRecoveryService delegating calculations to RecoveryPenaltyEngine
 * and transactions to RecoveryTransactionManager.
 */
public class ToolRecoveryServiceImpl implements ToolRecoveryService {

    private final ToolRepository toolRepository;
    private final ToolRecoveryRepository recoveryRepository;
    private final RecoveryPenaltyEngine penaltyEngine;
    private final RecoveryTransactionManager transactionManager;

    public ToolRecoveryServiceImpl(ToolRepository toolRepository,
                                  ToolRecoveryRepository recoveryRepository,
                                  RecoveryPenaltyEngine penaltyEngine,
                                  RecoveryTransactionManager transactionManager) {
        this.toolRepository = toolRepository;
        this.recoveryRepository = recoveryRepository;
        this.penaltyEngine = penaltyEngine;
        this.transactionManager = transactionManager;
    }

    @Override
    public boolean isRecoverable(UUID ownerUuid, ToolType toolType) {
        if (ownerUuid == null || toolType == null) return false;

        Optional<ToolProgression> progOpt = toolRepository.findByOwnerAndType(ownerUuid, toolType);
        if (!progOpt.isPresent()) return false;

        ToolProgression progression = progOpt.get();
        // Strictly LOST state ONLY
        return progression.getState() == ToolState.LOST;
    }

    @Override
    public RecoveryPreview createPreview(Player player, ToolProgression progression) {
        if (penaltyEngine == null) {
            return new RecoveryPreview(RecoveryPreview.Status.DISABLED, player != null ? player.getUniqueId() : null,
                    progression, null, 0, 0, 0.0, 0, 0, 0.0, 0.0, false,
                    null, false, 0, 0, "Penalty engine uninitialized.");
        }
        return penaltyEngine.createPreview(player, progression);
    }

    @Override
    public Optional<ToolRecoveryState> getRecoveryState(UUID toolUuid) {
        if (toolUuid == null || recoveryRepository == null) return Optional.empty();
        return recoveryRepository.findByToolUuid(toolUuid);
    }

    public RecoveryTransactionManager.TransactionResult executeRecovery(Player player, boolean adminForce) {
        if (transactionManager == null) {
            return new RecoveryTransactionManager.TransactionResult(
                    RecoveryTransactionManager.ResultType.FAILED_SYSTEM_ERROR,
                    "Transaction manager uninitialized.", null, null
            );
        }
        return transactionManager.executeRecovery(player, adminForce);
    }

    @Override
    public List<ToolRecoveryPenalty> getPendingPenalties(ToolProgression progression) {
        List<ToolRecoveryPenalty> penalties = new ArrayList<>();
        if (progression == null) return penalties;

        if (progression.getState() == ToolState.LOST) {
            RecoveryPreview preview = createPreview(null, progression);
            if (preview.getDurabilityPenaltyPercent() > 0) {
                penalties.add(new ToolRecoveryPenalty(
                        ToolRecoveryPenalty.Type.DURABILITY_PENALTY,
                        preview.getDurabilityPenaltyPercent(),
                        "Durability penalty applied: " + String.format("%.1f", preview.getDurabilityPenaltyPercent()) + "%"
                ));
            }
            if (preview.getCurrencyCost() > 0) {
                penalties.add(new ToolRecoveryPenalty(
                        ToolRecoveryPenalty.Type.CUSTOM,
                        preview.getCurrencyCost(),
                        "Currency requirement: $" + String.format("%.2f", preview.getCurrencyCost())
                ));
            }
        }

        return penalties;
    }

    @Override
    public Optional<ItemStack> prepareRecoveryItem(Player player, ToolProgression progression) {
        if (player == null || progression == null) return Optional.empty();
        if (!isRecoverable(player.getUniqueId(), progression.getToolType())) {
            return Optional.empty();
        }

        RecoveryTransactionManager.TransactionResult result = executeRecovery(player, false);
        if (result.isSuccess() && result.getRecoveredItem() != null) {
            return Optional.of(result.getRecoveredItem());
        }
        return Optional.empty();
    }
}
