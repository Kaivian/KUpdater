package io.github.kaivian.kupdater.features.tools.pickaxe.gui;

import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager.RecoveryConfig;
import io.github.kaivian.kupdater.features.tools.common.service.RecoveryTransactionManager;
import io.github.kaivian.kupdater.features.tools.common.service.RecoveryTransactionManager.TransactionResult;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Event listener enforcing GUI inventory safety and handling the merged action button
 * with optional 3-second confirmation countdown.
 */
public class PickaxeRecoveryGuiListener implements Listener {

    private final RecoveryTransactionManager transactionManager;
    private final ToolConfigManager configManager;
    private final Plugin plugin;

    public PickaxeRecoveryGuiListener(RecoveryTransactionManager transactionManager,
                                      ToolConfigManager configManager,
                                      Plugin plugin) {
        this.transactionManager = transactionManager;
        this.configManager = configManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null || !(event.getInventory().getHolder() instanceof PickaxeRecoveryHolder)) {
            return;
        }

        // Cancel all inventory click / move / steal interactions in the GUI unconditionally
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        PickaxeRecoveryHolder holder = (PickaxeRecoveryHolder) event.getInventory().getHolder();
        int rawSlot = event.getRawSlot();

        // Only react to the action button slot
        if (rawSlot != PickaxeRecoveryGui.SLOT_ACTION) {
            return;
        }

        // If recovery is unavailable (BARRIER), do nothing
        if (holder.getPreview() == null || !holder.getPreview().isAvailable()) {
            return;
        }

        // If already in confirming state → cancel the countdown and revert
        if (holder.isConfirming()) {
            holder.cancelConfirmation();
            // Revert button to the original "RECOVER PICKAXE" state
            PickaxeRecoveryGui.updateActionButton(event.getInventory(), createReadyButton());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c★ Recovery confirmation cancelled."));
            return;
        }

        RecoveryConfig cfg = configManager.getRecoveryConfig();
        boolean confirmEnabled = cfg != null && cfg.isConfirmOnRecovery();

        if (!confirmEnabled) {
            // Direct execution without confirm delay
            executeRecovery(player, event.getInventory());
            return;
        }

        // Start 3-second confirmation countdown
        holder.setConfirming(true);
        int delayTicks = cfg.getConfirmDelayTicks();
        int totalSeconds = Math.max(1, delayTicks / 20);

        // Show initial confirming button
        PickaxeRecoveryGui.updateActionButton(event.getInventory(), PickaxeRecoveryGui.createConfirmingButton(totalSeconds));

        // Schedule countdown ticks (update button every second)
        final Inventory guiInventory = event.getInventory();
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            private int remaining = totalSeconds;

            @Override
            public void run() {
                // Check if holder was cancelled or player closed GUI
                if (!holder.isConfirming() || !player.isOnline()) {
                    holder.cancelConfirmation();
                    return;
                }

                remaining--;

                if (remaining <= 0) {
                    // Time's up → execute recovery
                    holder.cancelConfirmation();
                    plugin.getServer().getScheduler().runTask(plugin, () -> executeRecovery(player, guiInventory));
                } else {
                    // Update countdown display
                    PickaxeRecoveryGui.updateActionButton(guiInventory, PickaxeRecoveryGui.createConfirmingButton(remaining));
                }
            }
        }, 20L, 20L); // Run every 20 ticks (1 second)

        holder.setConfirmTask(task);
    }

    /**
     * Execute the recovery transaction and close the GUI.
     */
    private void executeRecovery(Player player, Inventory inventory) {
        player.closeInventory();
        if (transactionManager != null) {
            TransactionResult result = transactionManager.executeRecovery(player, false);
            if (result.isSuccess()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a★ Pickaxe Recovery Successful! Your tool has been placed in your inventory."));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', result.getMessage()));
            }
        }
    }

    /**
     * Creates the default "ready to recover" button (same as PickaxeRecoveryGui creates).
     */
    private org.bukkit.inventory.ItemStack createReadyButton() {
        org.bukkit.Material mat;
        try {
            mat = org.bukkit.Material.valueOf("LIME_WOOL");
        } catch (Throwable t) {
            try {
                mat = org.bukkit.Material.valueOf("LIME_STAINED_GLASS_PANE");
            } catch (Throwable t2) {
                mat = org.bukkit.Material.EMERALD_BLOCK;
            }
        }
        org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(mat, 1);
        org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&l✔ RECOVER PICKAXE"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to recover your pickaxe."));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory() != null && event.getInventory().getHolder() instanceof PickaxeRecoveryHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != null && event.getInventory().getHolder() instanceof PickaxeRecoveryHolder) {
            PickaxeRecoveryHolder holder = (PickaxeRecoveryHolder) event.getInventory().getHolder();
            // Cancel any running confirmation countdown when GUI is closed
            holder.cancelConfirmation();
        }
    }
}
