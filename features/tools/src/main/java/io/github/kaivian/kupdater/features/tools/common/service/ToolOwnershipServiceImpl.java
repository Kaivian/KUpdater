package io.github.kaivian.kupdater.features.tools.common.service;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.service.ToolOwnershipService;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Implementation of ToolOwnershipService for validating tool ownership and restriction.
 */
public class ToolOwnershipServiceImpl implements ToolOwnershipService {

    private final ToolService toolService;
    private final ToolConfigManager configManager;

    public ToolOwnershipServiceImpl(ToolService toolService, ToolConfigManager configManager) {
        this.toolService = toolService;
        this.configManager = configManager;
    }

    @Override
    public boolean isOwner(Player player, ItemStack itemStack) {
        if (player == null || itemStack == null) return false;
        if (!toolService.isManagedTool(itemStack)) return true;

        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(itemStack);
        if (!progOpt.isPresent()) return false;

        ToolProgression progression = progOpt.get();
        return progression.getOwnerUuid().equals(player.getUniqueId());
    }

    @Override
    public boolean canUse(Player player, ItemStack itemStack) {
        if (player == null || itemStack == null) return true;
        if (!toolService.isManagedTool(itemStack)) return true;
        if (!isOwner(player, itemStack)) return false;

        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(itemStack);
        if (progOpt.isPresent()) {
            ToolProgression progression = progOpt.get();
            return progression.getState() == ToolState.ACTIVE;
        }
        return true;
    }

    @Override
    public String getOwnershipDenyMessage() {
        return configManager.getOwnershipDenyMessage();
    }
}
