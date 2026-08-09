package io.github.kaivian.kupdater.features.tools.common.listener;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.service.ToolDurabilityService;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Intercepts native item damage events to apply logical durability loss and prevent item destruction on exhaustion.
 */
public class ToolDurabilityListener implements Listener {

    private final ToolService toolService;
    private final ToolDurabilityService durabilityService;

    public ToolDurabilityListener(ToolService toolService, ToolDurabilityService durabilityService) {
        this.toolService = toolService;
        this.durabilityService = durabilityService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !toolService.isManagedTool(item)) return;

        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(item);
        if (!progOpt.isPresent()) return;

        // Cancel vanilla Minecraft item damage calculation to prevent physical item breakage/disappearance
        event.setCancelled(true);

        ToolProgression progression = progOpt.get();
        if (progression.getState() == io.github.kaivian.kupdater.api.tools.model.ToolState.DESTROYED || progression.getCurrentDurability() <= 0) {
            return;
        }

        durabilityService.applyDamage(item, progression, event.getDamage());
    }
}
