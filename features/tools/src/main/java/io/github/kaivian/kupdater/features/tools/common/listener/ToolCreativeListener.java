package io.github.kaivian.kupdater.features.tools.common.listener;

import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolStat;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles Creative Mode item spawning from the Creative Inventory tab.
 * <p>
 * Runs at LOWEST priority so it executes BEFORE PickaxeRegistrationListener (LOW).
 * Checks the raw item for KUpdater PDC metadata BEFORE registration can stamp it.
 * <p>
 * - Admin with override permission: overrides DB progression to match grabbed material (upgrade or downgrade).
 * - Normal player: forces item to match their current DB progression record.
 */
public class ToolCreativeListener implements Listener {

    private final ToolService toolService;
    private final ToolRepository repository;
    private final ToolConfigManager configManager;
    private final Plugin plugin;
    private final Logger logger;

    /** PDC key used by ToolItemMetadataService to mark managed tools */
    private final NamespacedKey toolUuidKey;

    public ToolCreativeListener(ToolService toolService, ToolRepository repository, ToolConfigManager configManager, Plugin plugin) {
        this.toolService = toolService;
        this.repository = repository;
        this.configManager = configManager;
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger("ToolCreativeListener");
        this.toolUuidKey = plugin != null ? new NamespacedKey(plugin, "tool_uuid") : null;
    }

    /**
     * Check if item has KUpdater PDC metadata by directly inspecting the PersistentDataContainer.
     * This avoids relying on toolService.isManagedTool() which could be affected by event ordering.
     */
    private boolean hasKUpdaterMetadata(ItemStack item) {
        if (item == null || !item.hasItemMeta() || toolUuidKey == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(toolUuidKey, PersistentDataType.STRING);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreativeInventory(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // InventoryCreativeEvent: getCursor() is the item the client is placing into the slot
        ItemStack item = event.getCursor();
        if (item == null || item.getType() == Material.AIR) return;

        // Only handle pickaxe materials
        if (!item.getType().name().endsWith("_PICKAXE")) return;

        ToolMaterial grabbedMaterial = getToolMaterialFromBukkit(item.getType());
        if (grabbedMaterial == null) return;

        // If item already has KUpdater PDC metadata, it's an existing managed tool -> skip
        if (hasKUpdaterMetadata(item)) {
            return;
        }

        // If a managed tool is being destroyed/cleared in Creative tab
        if ((item == null || item.getType() == Material.AIR) && hasKUpdaterMetadata(event.getCurrentItem())) {
            Optional<UUID> toolUuidOpt = toolService.getToolUuidFromItem(event.getCurrentItem());
            toolUuidOpt.ifPresent(uuid -> toolService.updateState(uuid, ToolState.LOST));
            return;
        }

        logger.info("[ToolCreative] Player " + player.getName() + " spawned raw " + item.getType().name()
                + " from Creative. hasOverride=" + (player.hasPermission("kupdater.tool.override") || player.hasPermission("kupdater.admin") || player.isOp()));

        boolean hasOverridePermission = player.hasPermission("kupdater.tool.override")
                || player.hasPermission("kupdater.admin")
                || player.isOp();

        Optional<ToolProgression> progOpt = repository.findByOwnerAndType(player.getUniqueId(), ToolType.PICKAXE);

        if (hasOverridePermission) {
            // Admin with override permission -> override DB progression to match grabbed material (upgrade or downgrade)
            int level = 1;
            ToolStat stat = configManager.getStat(grabbedMaterial, level);
            int maxDur = stat != null ? stat.getMaxDurability() : 100;

            ToolProgression updatedProg;
            if (progOpt.isPresent()) {
                ToolProgression existing = progOpt.get();
                updatedProg = new ToolProgression(
                        existing.getToolUuid(),
                        player.getUniqueId(),
                        ToolType.PICKAXE,
                        grabbedMaterial,
                        level,
                        0,
                        0,
                        ToolState.ACTIVE,
                        maxDur,
                        existing.getSchemaVersion() + 1,
                        existing.getCreatedAt(),
                        Instant.now()
                );
            } else {
                updatedProg = new ToolProgression(
                        UUID.randomUUID(),
                        player.getUniqueId(),
                        ToolType.PICKAXE,
                        grabbedMaterial,
                        level,
                        0,
                        0,
                        ToolState.ACTIVE,
                        maxDur,
                        1,
                        Instant.now(),
                        Instant.now()
                );
            }

            repository.save(updatedProg);

            // Remove any OLD managed pickaxes from inventory before placing the new one
            removeOldManagedPickaxes(player, event.getSlot());

            // Apply material + metadata to the item and update the event cursor
            Material targetBukkitMat = configManager.getBukkitMaterial(grabbedMaterial);
            item.setType(targetBukkitMat);
            toolService.applyMetadataToItem(item, updatedProg);
            event.setCursor(item);

            String overrideMsg = "&a★ [Admin] Overrode tool progression in database to &e"
                    + grabbedMaterial.getDisplayName() + " Pickaxe Level 1&a!";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', overrideMsg));

            logger.info("[ToolCreative] Admin " + player.getName() + " overrode progression to " + grabbedMaterial.name() + " Level 1");
        } else {
            // Normal player -> force item to match current DB progression
            ToolProgression dbProg;
            if (progOpt.isPresent()) {
                dbProg = progOpt.get();
            } else {
                ToolStat lvl1Stat = configManager.getStat(ToolMaterial.WOODEN, 1);
                int maxDurability = lvl1Stat != null ? lvl1Stat.getMaxDurability() : 59;
                dbProg = new ToolProgression(
                        UUID.randomUUID(),
                        player.getUniqueId(),
                        ToolType.PICKAXE,
                        ToolMaterial.WOODEN,
                        1,
                        0,
                        0,
                        ToolState.ACTIVE,
                        maxDurability,
                        1,
                        Instant.now(),
                        Instant.now()
                );
                repository.save(dbProg);
            }

            // Remove any OLD managed pickaxes from inventory before placing the new one
            removeOldManagedPickaxes(player, event.getSlot());

            Material targetBukkitMat = configManager.getBukkitMaterial(dbProg.getMaterial());
            item.setType(targetBukkitMat);
            toolService.applyMetadataToItem(item, dbProg);
            event.setCursor(item);

            logger.info("[ToolCreative] Normal player " + player.getName() + " forced item to match DB: " + dbProg.getMaterial().name() + " Level " + dbProg.getLevel());
        }

        // Schedule inventory update on next tick so client texture matches
        if (plugin != null && plugin.isEnabled()) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, player::updateInventory);
        }
    }

    /**
     * Removes all existing KUpdater-managed pickaxes from the player's inventory,
     * EXCEPT the slot being set by the current Creative event.
     * This ensures only one managed pickaxe exists at a time.
     */
    private void removeOldManagedPickaxes(Player player, int excludeSlot) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (i == excludeSlot) continue;
            ItemStack slot = inv.getItem(i);
            if (slot != null && slot.getType().name().endsWith("_PICKAXE") && hasKUpdaterMetadata(slot)) {
                inv.setItem(i, new ItemStack(Material.AIR));
            }
        }
    }

    private ToolMaterial getToolMaterialFromBukkit(Material mat) {
        if (mat == null) return null;
        for (ToolMaterial tm : ToolMaterial.values()) {
            Material bMat = configManager.getBukkitMaterial(tm);
            if (bMat == mat) {
                return tm;
            }
        }
        return null;
    }
}
