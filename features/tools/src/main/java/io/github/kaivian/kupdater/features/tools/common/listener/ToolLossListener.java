package io.github.kaivian.kupdater.features.tools.common.listener;

import io.github.kaivian.kupdater.api.tools.model.ToolState;

import io.github.kaivian.kupdater.api.tools.service.ToolService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * Tracks tool loss (e.g. item despawn) to update database progression state to LOST.
 */
public class ToolLossListener implements Listener {

    private final ToolService toolService;

    public ToolLossListener(ToolService toolService) {
        this.toolService = toolService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        if (item == null || !toolService.isManagedTool(item)) return;

        Optional<UUID> toolUuidOpt = toolService.getToolUuidFromItem(item);
        toolUuidOpt.ifPresent(uuid -> toolService.updateState(uuid, ToolState.LOST));
    }
}
