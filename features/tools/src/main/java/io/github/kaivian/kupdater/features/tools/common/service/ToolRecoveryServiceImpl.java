package io.github.kaivian.kupdater.features.tools.common.service;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryPenalty;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolRecoveryService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of ToolRecoveryService for recovering lost or destroyed tools.
 */
public class ToolRecoveryServiceImpl implements ToolRecoveryService {

    private final ToolRepository repository;

    public ToolRecoveryServiceImpl(ToolRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isRecoverable(UUID ownerUuid, ToolType toolType) {
        if (ownerUuid == null || toolType == null) return false;

        Optional<ToolProgression> progOpt = repository.findByOwnerAndType(ownerUuid, toolType);
        if (!progOpt.isPresent()) return false;

        ToolProgression progression = progOpt.get();
        return progression.getState() == ToolState.LOST || progression.getState() == ToolState.DESTROYED;
    }

    @Override
    public List<ToolRecoveryPenalty> getPendingPenalties(ToolProgression progression) {
        List<ToolRecoveryPenalty> penalties = new ArrayList<>();
        if (progression == null) return penalties;

        if (progression.getState() == ToolState.DESTROYED) {
            penalties.add(new ToolRecoveryPenalty(ToolRecoveryPenalty.Type.DURABILITY_PENALTY, 0, "Durability reset to default upon recovery"));
        } else if (progression.getState() == ToolState.LOST) {
            penalties.add(new ToolRecoveryPenalty(ToolRecoveryPenalty.Type.LEVEL_PENALTY, 1, "Level penalty required to recover lost tool"));
        }

        return penalties;
    }

    @Override
    public Optional<ItemStack> prepareRecoveryItem(Player player, ToolProgression progression) {
        if (player == null || progression == null) return Optional.empty();
        if (!isRecoverable(player.getUniqueId(), progression.getToolType())) {
            return Optional.empty();
        }

        ToolProgression recovered = progression.withState(ToolState.ACTIVE);
        repository.save(recovered);

        ItemStack item = new ItemStack(progression.getMaterial().getBukkitMaterial());
        return Optional.of(item);
    }
}
