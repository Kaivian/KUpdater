package io.github.kaivian.kupdater.features.tools.pickaxe.listener;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Auto-registers initial vanilla Pickaxes for players without existing progression.
 */
public class PickaxeRegistrationListener implements Listener {

    private final ToolService toolService;
    private final Plugin plugin;

    public PickaxeRegistrationListener(ToolService toolService) {
        this(toolService, null);
    }

    public PickaxeRegistrationListener(ToolService toolService, Plugin plugin) {
        this.toolService = toolService;
        this.plugin = plugin;
    }

    private boolean isPickaxe(Material material) {
        if (material == null) return false;
        return material.name().endsWith("_PICKAXE");
    }

    private void scheduleInventoryUpdate(Player player) {
        if (player == null) return;
        if (plugin != null && plugin.isEnabled()) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, player::updateInventory);
        } else {
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onCreativeInventory(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        boolean modified = false;

        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && cursorItem.getType() != Material.AIR && isPickaxe(cursorItem.getType())) {
            if (!toolService.isManagedTool(cursorItem)) {
                Optional<ToolProgression> progOpt = toolService.registerInitialTool(player, cursorItem);
                if (progOpt.isPresent()) {
                    event.setCursor(cursorItem);
                    modified = true;
                }
            }
        }

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && currentItem.getType() != Material.AIR && isPickaxe(currentItem.getType())) {
            if (!toolService.isManagedTool(currentItem)) {
                Optional<ToolProgression> progOpt = toolService.registerInitialTool(player, currentItem);
                if (progOpt.isPresent()) {
                    event.setCurrentItem(currentItem);
                    modified = true;
                }
            }
        }

        if (modified) {
            scheduleInventoryUpdate(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event instanceof InventoryCreativeEvent) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        boolean modified = false;

        ItemStack current = event.getCurrentItem();
        if (current != null && current.getType() != Material.AIR && isPickaxe(current.getType())) {
            if (!toolService.isManagedTool(current)) {
                Optional<ToolProgression> progOpt = toolService.registerInitialTool(player, current);
                if (progOpt.isPresent()) {
                    event.setCurrentItem(current);
                    modified = true;
                }
            }
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR && isPickaxe(cursor.getType())) {
            if (!toolService.isManagedTool(cursor)) {
                Optional<ToolProgression> progOpt = toolService.registerInitialTool(player, cursor);
                if (progOpt.isPresent()) {
                    event.setCursor(cursor);
                    modified = true;
                }
            }
        }

        if (modified) {
            scheduleInventoryUpdate(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getCurrentItem();
        if (result == null || !isPickaxe(result.getType())) return;

        if (!toolService.isManagedTool(result)) {
            Optional<ToolProgression> progOpt = toolService.registerInitialTool(player, result);
            if (progOpt.isPresent()) {
                event.setCurrentItem(result);
                scheduleInventoryUpdate(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        if (item == null || !isPickaxe(item.getType())) return;

        if (!toolService.isManagedTool(item)) {
            Optional<ToolProgression> progOpt = toolService.registerInitialTool(player, item);
            if (progOpt.isPresent()) {
                event.getItem().setItemStack(item);
                scheduleInventoryUpdate(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !isPickaxe(item.getType())) return;

        if (!toolService.isManagedTool(item)) {
            Optional<ToolProgression> progOpt = toolService.registerInitialTool(event.getPlayer(), item);
            if (progOpt.isPresent()) {
                event.getPlayer().getInventory().setItemInMainHand(item);
                scheduleInventoryUpdate(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !isPickaxe(item.getType())) return;

        if (!toolService.isManagedTool(item)) {
            Optional<ToolProgression> progOpt = toolService.registerInitialTool(player, item);
            if (progOpt.isPresent()) {
                player.getInventory().setItemInMainHand(item);
                scheduleInventoryUpdate(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item == null || !isPickaxe(item.getType())) return;

        if (!toolService.isManagedTool(item)) {
            Optional<ToolProgression> progOpt = toolService.registerInitialTool(player, item);
            if (progOpt.isPresent()) {
                player.getInventory().setItem(event.getNewSlot(), item);
                scheduleInventoryUpdate(player);
            }
        }
    }
}
