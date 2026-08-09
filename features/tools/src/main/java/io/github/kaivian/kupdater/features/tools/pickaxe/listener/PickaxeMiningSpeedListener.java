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
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * Dynamically applies Mining Speed attribute to players ONLY when mining Pickaxe-suitable blocks.
 * Block eligibility is determined by the {@code mineable-blocks} list in pickaxe.yml config.
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        removeMiningSpeedModifier(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        removeMiningSpeedModifier(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeMiningSpeedModifier(event.getPlayer());
    }

    private void applyMiningSpeedModifier(Player player, double bonus) {
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
