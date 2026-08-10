package io.github.kaivian.kupdater.features.tools.common.listener;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * Tracks physical tool loss in the game world (despawn, lava burn, void fall, explosion, cactus)
 * to update database progression state to LOST.
 */
public class ToolLossListener implements Listener {

    private final ToolService toolService;

    public ToolLossListener(ToolService toolService) {
        this.toolService = toolService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (event.getEntity() == null) return;
        ItemStack item = event.getEntity().getItemStack();
        markAsLostIfManaged(item);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item)) return;

        Item itemEntity = (Item) event.getEntity();
        ItemStack item = itemEntity.getItemStack();
        if (item == null || !toolService.isManagedTool(item)) return;

        // Check if damage will destroy the item entity or cause cause is environmental destruction
        EntityDamageEvent.DamageCause cause = event.getCause();
        boolean isDestructiveCause = cause == EntityDamageEvent.DamageCause.LAVA ||
                cause == EntityDamageEvent.DamageCause.FIRE ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                cause == EntityDamageEvent.DamageCause.VOID ||
                cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                cause == EntityDamageEvent.DamageCause.CONTACT;

        if (isDestructiveCause || (itemEntity.getHealth() - event.getFinalDamage() <= 0)) {
            markAsLostIfManaged(item);
        }
    }

    private void markAsLostIfManaged(ItemStack item) {
        if (item == null || !toolService.isManagedTool(item)) return;

        Optional<UUID> itemToolUuidOpt = toolService.getToolUuidFromItem(item);
        if (!itemToolUuidOpt.isPresent()) return;

        UUID itemToolUuid = itemToolUuidOpt.get();
        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(item);

        // If item's toolUuid does not exist in DB (e.g. old discarded pickaxe after recovery), ignore it!
        if (!progOpt.isPresent()) {
            return;
        }

        ToolProgression currentProg = progOpt.get();
        if (currentProg.getToolUuid().equals(itemToolUuid)) {
            toolService.updateState(itemToolUuid, ToolState.LOST);
        }
    }
}
