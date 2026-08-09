package io.github.kaivian.kupdater.api.tools.service;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Service contract for centralizing tool ownership validation across event listeners.
 */
public interface ToolOwnershipService {

    /**
     * Checks if the specified player is the registered owner of the managed tool.
     *
     * @param player    player attempting action
     * @param itemStack item stack being checked
     * @return true if player is owner or item is unmanaged
     */
    boolean isOwner(Player player, ItemStack itemStack);

    /**
     * Checks if the specified player is authorized to use the tool for gameplay actions
     * (mining blocks, hitting entities, interacting).
     *
     * @param player    player attempting action
     * @param itemStack item stack being used
     * @return true if authorized to use, false if denied
     */
    boolean canUse(Player player, ItemStack itemStack);

    /**
     * Gets the configured user-facing deny message when an unauthorized player attempts to use a tool.
     *
     * @return ownership denial message string
     */
    String getOwnershipDenyMessage();
}
