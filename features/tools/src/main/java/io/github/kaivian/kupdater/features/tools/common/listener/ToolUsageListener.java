package io.github.kaivian.kupdater.features.tools.common.listener;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.service.ToolOwnershipService;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Enforces ownership authorization and broken tool restrictions on tool usage.
 */
public class ToolUsageListener implements Listener {

    private final ToolService toolService;
    private final ToolOwnershipService ownershipService;
    private final ToolConfigManager configManager;

    public ToolUsageListener(ToolService toolService, ToolOwnershipService ownershipService, ToolConfigManager configManager) {
        this.toolService = toolService;
        this.ownershipService = ownershipService;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        checkAndEnforceUsage(event, player, item);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        checkAndEnforceUsage(event, player, item);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        checkAndEnforceUsage(event, player, item);
    }

    private boolean isPickaxe(Material material) {
        if (material == null) return false;
        return material.name().endsWith("_PICKAXE");
    }

    private void checkAndEnforceUsage(org.bukkit.event.Cancellable event, Player player, ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return;
        if (!isPickaxe(item.getType())) return;

        if (!toolService.isManagedTool(item)) {
            toolService.registerInitialTool(player, item);
        }
        if (!toolService.isManagedTool(item)) return;

        if (!ownershipService.isOwner(player, item)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getOwnershipDenyMessage()));
            return;
        }

        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(item);
        if (progOpt.isPresent()) {
            ToolProgression progression = progOpt.get();
            if (progression.getState() == ToolState.DESTROYED) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getToolBrokenMessage()));
            } else if (progression.getState() == ToolState.LOST) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getToolLostMessage()));
            }
        } else {
            event.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getInvalidToolMessage()));
        }
    }
}
