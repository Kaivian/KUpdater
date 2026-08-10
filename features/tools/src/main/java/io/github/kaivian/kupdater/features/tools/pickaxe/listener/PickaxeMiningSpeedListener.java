package io.github.kaivian.kupdater.features.tools.pickaxe.listener;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolStat;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * Dynamically applies Mining Speed attribute to players ONLY when mining Pickaxe-suitable blocks.
 * Block eligibility is determined by the {@code mineable-blocks} list in pickaxe.yml config.
 * <p>
 * Ensures the modifier is removed in ALL cases where the player's held item changes or becomes invalid:
 * - Block break completed
 * - Hotbar slot change (scroll wheel or number keys)
 * - Inventory click (moving items in/out of hotbar)
 * - Player death
 * - Player respawn
 * - Item drop (Q key)
 * - Hand swap (F key)
 * - Gamemode change
 * - Player quit
 */
public class PickaxeMiningSpeedListener implements Listener {

    private static final String MODIFIER_NAME = "kupdater_mining_speed";

    private final ToolService toolService;
    private final ToolConfigManager configManager;

    public PickaxeMiningSpeedListener(ToolService toolService, ToolConfigManager configManager) {
        this.toolService = toolService;
        this.configManager = configManager;
    }

    private boolean isPickaxe(Material material) {
        if (material == null) return false;
        return material.name().endsWith("_PICKAXE");
    }

    /**
     * Returns true ONLY if the player is currently holding a managed KUpdater pickaxe in their MAIN HAND.
     */
    private boolean isHoldingManagedPickaxeMainHand(Player player) {
        if (player == null) return false;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return mainHand != null
                && !mainHand.getType().isAir()
                && isPickaxe(mainHand.getType())
                && toolService.isManagedTool(mainHand);
    }

    // ==================== APPLY MODIFIER ====================

    /**
     * Apply mining speed modifier when the player starts damaging a block.
     * Only applies if the player is holding a managed pickaxe and the block is mineable.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !isPickaxe(item.getType()) || !toolService.isManagedTool(item)) {
            removeMiningSpeedModifier(player);
            return;
        }

        if (configManager != null && !configManager.isPickaxeMineableBlock(event.getBlock().getType())) {
            removeMiningSpeedModifier(player);
            return;
        }

        Optional<ToolProgression> progOpt = toolService.getProgressionFromItem(item);
        if (!progOpt.isPresent()) {
            removeMiningSpeedModifier(player);
            return;
        }

        ToolProgression progression = progOpt.get();
        ToolStat stat = configManager.getStat(progression.getMaterial(), progression.getLevel());
        double targetTotalSpeed = stat != null ? stat.getMiningSpeedMultiplier() : 1.0;
        double vanillaBaseSpeed = getVanillaBaseSpeed(item.getType());
        double bonus = (targetTotalSpeed / vanillaBaseSpeed) - 1.0;

        applyMiningSpeedModifier(player, bonus);
    }

    // ==================== REMOVE MODIFIER — ALL CASES ====================

    /**
     * Remove modifier after block is fully broken.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        removeMiningSpeedModifier(event.getPlayer());
    }

    /**
     * Remove modifier when player changes held slot (scroll wheel OR number key).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        removeMiningSpeedModifier(event.getPlayer());
    }

    /**
     * Remove modifier when player clicks inside inventory.
     * Covers: moving items in/out of hotbar, number key swaps inside inventory screen,
     * shift-clicking items, etc.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Only need to clean up if the click could affect the main hand slot
        int heldSlot = player.getInventory().getHeldItemSlot();

        // Direct click on held slot
        boolean affectsHeldSlot = event.getSlot() == heldSlot
                && event.getClickedInventory() == player.getInventory();

        // Number key hotbar swap (e.g. pressing 1-9 while hovering an item in inventory)
        boolean isHotbarSwap = event.getClick().name().contains("NUMBER_KEY")
                && event.getHotbarButton() == heldSlot;

        // Shift-click could move items into or out of the held slot
        boolean isShiftClick = event.isShiftClick();

        if (affectsHeldSlot || isHotbarSwap || isShiftClick) {
            removeMiningSpeedModifier(player);
        }
    }

    /**
     * Remove modifier when player dies.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        removeMiningSpeedModifier(event.getEntity());
    }

    /**
     * Remove modifier when player respawns (safety net after death).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        removeMiningSpeedModifier(event.getPlayer());
    }

    /**
     * Remove modifier when player drops an item (Q key).
     * The held item may change if it was the last one in the stack.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        removeMiningSpeedModifier(event.getPlayer());
    }

    /**
     * Remove modifier when player swaps main/off hand (F key).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        removeMiningSpeedModifier(event.getPlayer());
    }

    /**
     * Remove modifier when player changes gamemode (e.g. survival -> creative).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        removeMiningSpeedModifier(event.getPlayer());
    }

    /**
     * Remove modifier when player quits (cleanup).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeMiningSpeedModifier(event.getPlayer());
    }

    // ==================== VANILLA BASE SPEED MAP ====================

    private double getVanillaBaseSpeed(Material material) {
        if (material == null) return 1.0;
        if ("COPPER_PICKAXE".equalsIgnoreCase(material.name())) {
            return 5.0;
        }
        switch (material) {
            case WOODEN_PICKAXE:
                return 2.0;
            case STONE_PICKAXE:
                return 4.0;
            case IRON_PICKAXE:
                return 6.0;
            case GOLDEN_PICKAXE:
                return 12.0;
            case DIAMOND_PICKAXE:
                return 8.0;
            case NETHERITE_PICKAXE:
                return 9.0;
            default:
                return 1.0;
        }
    }

    // ==================== MODIFIER APPLY / REMOVE ====================

    private void applyMiningSpeedModifier(Player player, double bonus) {
        // Final safety gate: only apply if a managed pickaxe is actually in main hand
        if (!isHoldingManagedPickaxeMainHand(player)) {
            removeMiningSpeedModifier(player);
            return;
        }

        AttributeInstance attrInst = getBreakSpeedAttributeInstance(player);
        if (attrInst == null) return;

        removeMiningSpeedModifier(player);

        if (Math.abs(bonus) <= 0.0001) return;

        AttributeModifier modifier;
        try {
            modifier = new AttributeModifier(
                    new NamespacedKey("kupdater", "mining_speed_attr"),
                    bonus,
                    AttributeModifier.Operation.ADD_NUMBER,
                    org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
            );
        } catch (Throwable t) {
            UUID uuid = UUID.nameUUIDFromBytes(MODIFIER_NAME.getBytes());
            modifier = new AttributeModifier(
                    uuid,
                    MODIFIER_NAME,
                    bonus,
                    AttributeModifier.Operation.ADD_NUMBER,
                    org.bukkit.inventory.EquipmentSlot.HAND
            );
        }

        try {
            attrInst.addModifier(modifier);
        } catch (Throwable ignored) {
        }
    }

    private void removeMiningSpeedModifier(Player player) {
        AttributeInstance attrInst = getBreakSpeedAttributeInstance(player);
        if (attrInst == null) return;

        try {
            for (AttributeModifier mod : attrInst.getModifiers()) {
                boolean matchesKey = false;
                try {
                    matchesKey = mod.getKey() != null && "kupdater".equals(mod.getKey().getNamespace()) && "mining_speed_attr".equals(mod.getKey().getKey());
                } catch (Throwable ignored) {
                }
                if (matchesKey || MODIFIER_NAME.equalsIgnoreCase(mod.getName())) {
                    attrInst.removeModifier(mod);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private AttributeInstance getBreakSpeedAttributeInstance(Player player) {
        if (player == null) return null;
        for (Attribute attr : Attribute.values()) {
            String name = attr.name();
            if (name.equalsIgnoreCase("PLAYER_BLOCK_BREAK_SPEED") ||
                name.equalsIgnoreCase("GENERIC_BLOCK_BREAK_SPEED") ||
                name.equalsIgnoreCase("BLOCK_BREAK_SPEED")) {
                try {
                    return player.getAttribute(attr);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }
}
