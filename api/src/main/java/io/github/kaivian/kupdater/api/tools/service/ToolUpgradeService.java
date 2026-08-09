package io.github.kaivian.kupdater.api.tools.service;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Service contract for validating requirements and executing tool level upgrades.
 */
public interface ToolUpgradeService {

    /**
     * Validates whether a player can upgrade their currently held tool to the next level.
     *
     * @param player    player attempting upgrade
     * @param itemStack item stack being upgraded
     * @return true if upgrade requirements are satisfied
     */
    boolean canUpgrade(Player player, ItemStack itemStack);

    /**
     * Executes tool upgrade to the next level.
     * Updates database persistence, item PDC metadata, lore, and fires ToolUpgradeEvent.
     *
     * @param player    player executing upgrade
     * @param itemStack item stack being upgraded
     * @return Optional containing updated ToolProgression if upgrade succeeded
     */
    Optional<ToolProgression> upgradeTool(Player player, ItemStack itemStack);

    /**
     * Executes material tier upgrade for a pickaxe at max level & max XP via crafting.
     *
     * @param player    player executing tier upgrade
     * @param itemStack item stack being upgraded
     * @return Optional containing updated ToolProgression if tier upgrade succeeded
     */
    Optional<ToolProgression> upgradeTier(Player player, ItemStack itemStack);

    /**
     * Gets the configured maximum level for a tool category.
     *
     * @param toolType tool category string or name
     * @return maximum configurable level integer
     */
    int getMaxLevel(String toolType);
}
