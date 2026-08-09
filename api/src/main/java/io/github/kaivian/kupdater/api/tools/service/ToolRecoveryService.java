package io.github.kaivian.kupdater.api.tools.service;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryPenalty;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service contract / extension point for future tool recovery implementation.
 */
public interface ToolRecoveryService {

    /**
     * Checks if a player has a tool in LOST or DESTROYED state available for future recovery.
     *
     * @param ownerUuid player UUID
     * @param toolType  tool category
     * @return true if recoverable progression exists
     */
    boolean isRecoverable(UUID ownerUuid, ToolType toolType);

    /**
     * Resolves pending recovery penalties for a tool progression.
     *
     * @param progression tool progression
     * @return list of recovery penalties to apply upon recovery execution
     */
    List<ToolRecoveryPenalty> getPendingPenalties(ToolProgression progression);

    /**
     * Prepares recovery parameters for restoring a LOST or DESTROYED tool progression.
     * Note: Full recovery crafting flow is implemented by the future Recovery module.
     *
     * @param player      owner player
     * @param progression tool progression being recovered
     * @return Optional containing recovered tool item stack representation
     */
    Optional<ItemStack> prepareRecoveryItem(Player player, ToolProgression progression);
}
