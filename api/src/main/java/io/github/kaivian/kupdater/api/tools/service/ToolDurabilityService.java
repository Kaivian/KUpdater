package io.github.kaivian.kupdater.api.tools.service;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import org.bukkit.inventory.ItemStack;

/**
 * Version-independent service contract for managing logical tool durability and durability exhaustion.
 */
public interface ToolDurabilityService {

    /**
     * Calculates maximum logical durability for a given tool level.
     *
     * @param toolType tool type string or enum
     * @param level    tool progression level
     * @return maximum durability integer
     */
    int getMaxDurability(String toolType, int level);

    /**
     * Applies logical durability damage to a tool item stack.
     * If durability is exhausted, native Minecraft item removal is prevented, the item remains unusable in inventory/world,
     * and state transitions to DESTROYED in database.
     *
     * @param itemStack   item stack receiving damage
     * @param progression authoritative progression object
     * @param amount      damage amount to apply
     * @return updated ToolProgression instance
     */
    ToolProgression applyDamage(ItemStack itemStack, ToolProgression progression, int amount);

    /**
     * Checks if a managed tool item stack has exhausted its logical durability (is DESTROYED).
     *
     * @param itemStack item stack to check
     * @return true if durability is exhausted / tool is broken
     */
    boolean isExhausted(ItemStack itemStack);
}
