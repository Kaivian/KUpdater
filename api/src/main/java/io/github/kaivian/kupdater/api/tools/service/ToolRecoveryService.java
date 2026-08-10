package io.github.kaivian.kupdater.api.tools.service;

import io.github.kaivian.kupdater.api.tools.model.RecoveryPreview;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryPenalty;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service contract for managing pickaxe recovery evaluation, preview generation, and transaction execution.
 */
public interface ToolRecoveryService {

    /**
     * Checks if a player has a tool in LOST state available for recovery.
     *
     * @param ownerUuid player UUID
     * @param toolType  tool category
     * @return true if recoverable progression in LOST state exists
     */
    boolean isRecoverable(UUID ownerUuid, ToolType toolType);

    /**
     * Creates a server-calculated preview for tool recovery.
     *
     * @param player      owner player
     * @param progression tool progression
     * @return RecoveryPreview object
     */
    RecoveryPreview createPreview(Player player, ToolProgression progression);

    /**
     * Gets persistent recovery state for a tool UUID.
     *
     * @param toolUuid tool UUID
     * @return Optional containing ToolRecoveryState if found
     */
    Optional<ToolRecoveryState> getRecoveryState(UUID toolUuid);

    /**
     * Resolves pending recovery penalties for a tool progression.
     *
     * @param progression tool progression
     * @return list of recovery penalties to apply upon recovery execution
     */
    List<ToolRecoveryPenalty> getPendingPenalties(ToolProgression progression);

    /**
     * Prepares recovery item representation.
     *
     * @param player      owner player
     * @param progression tool progression being recovered
     * @return Optional containing recovered tool item stack representation
     */
    Optional<ItemStack> prepareRecoveryItem(Player player, ToolProgression progression);
}
