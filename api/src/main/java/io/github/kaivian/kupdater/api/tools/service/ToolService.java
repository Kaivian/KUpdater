package io.github.kaivian.kupdater.api.tools.service;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * Primary service contract for managing tool progression, metadata tagging, and lifecycle transitions.
 */
public interface ToolService {

    /**
     * Resolves tool progression for a player and tool type from database.
     *
     * @param ownerUuid player UUID
     * @param toolType  tool type
     * @return Optional containing tool progression if found
     */
    Optional<ToolProgression> getProgression(UUID ownerUuid, ToolType toolType);

    /**
     * Resolves tool progression by unique tool UUID.
     *
     * @param toolUuid unique tool UUID
     * @return Optional containing tool progression if found
     */
    Optional<ToolProgression> getProgressionByToolUuid(UUID toolUuid);

    /**
     * Resolves tool progression from an ItemStack's persistent metadata and validates against database.
     *
     * @param itemStack item stack
     * @return Optional containing authoritative tool progression if valid
     */
    Optional<ToolProgression> getProgressionFromItem(ItemStack itemStack);

    /**
     * Attempts to auto-register an unmanaged vanilla tool item for a player.
     * Registration ONLY occurs if the player has no existing progression record for the specified tool type.
     *
     * @param player    player instance
     * @param itemStack unmanaged vanilla item
     * @return Optional containing newly created progression, or empty if player already owns a progression
     */
    Optional<ToolProgression> registerInitialTool(Player player, ItemStack itemStack);

    /**
     * Updates an ItemStack's PDC metadata and lore to match authoritative database state.
     *
     * @param itemStack   item stack to stamp
     * @param progression authoritative progression state
     */
    void applyMetadataToItem(ItemStack itemStack, ToolProgression progression);

    /**
     * Transitions a tool's lifecycle state (ACTIVE, LOST, DESTROYED).
     *
     * @param toolUuid tool UUID
     * @param newState target state
     * @return true if transition succeeded
     */
    boolean updateState(UUID toolUuid, ToolState newState);

    /**
     * Checks if an ItemStack carries managed KUpdater metadata.
     *
     * @param itemStack item stack to check
     * @return true if item is tagged as a managed tool
     */
    boolean isManagedTool(ItemStack itemStack);

    /**
     * Reads tool UUID stored in item metadata without database lookup.
     *
     * @param itemStack item stack
     * @return Optional containing tool UUID if present
     */
    Optional<UUID> getToolUuidFromItem(ItemStack itemStack);
}
