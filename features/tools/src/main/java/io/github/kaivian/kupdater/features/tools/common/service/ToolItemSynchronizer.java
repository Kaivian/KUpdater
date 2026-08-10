package io.github.kaivian.kupdater.features.tools.common.service;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.metadata.ToolItemMetadataService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service responsible for physical ItemStack PDC metadata, display name, and lore synchronization.
 * Guarantees that all inventory and item mutations execute strictly on the Bukkit main server thread.
 */
public class ToolItemSynchronizer {

    private final Plugin plugin;
    private final ToolService toolService;
    private final ToolItemMetadataService metadataService;
    private final ToolConfigManager configManager;
    private final Logger logger;

    public ToolItemSynchronizer(Plugin plugin, ToolService toolService,
                                ToolItemMetadataService metadataService, ToolConfigManager configManager,
                                Logger logger) {
        this.plugin = plugin;
        this.toolService = Objects.requireNonNull(toolService, "toolService cannot be null");
        this.metadataService = Objects.requireNonNull(metadataService, "metadataService cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
        this.logger = logger != null ? logger : Logger.getLogger("ToolItemSynchronizer");
    }

    /**
     * Synchronizes a player's held or inventory tool with the authoritative DB progression record.
     * Safely dispatches to the main server thread if invoked asynchronously.
     *
     * @param player      target player
     * @param progression authoritative progression state
     */
    public void synchronizePlayerTool(Player player, ToolProgression progression) {
        if (player == null || progression == null || !player.isOnline()) return;

        Runnable syncTask = () -> {
            boolean updated = false;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType() == Material.AIR) continue;
                if (!metadataService.isManagedTool(item)) continue;

                Optional<java.util.UUID> toolUuidOpt = metadataService.getToolUuid(item);
                if (toolUuidOpt.isPresent() && toolUuidOpt.get().equals(progression.getToolUuid())) {
                    if (progression.getState() == ToolState.LOST || progression.getState() == ToolState.DESTROYED) {
                        item.setAmount(0); // Revoke/remove item if state is LOST/DESTROYED
                    } else {
                        // Stamp new material and metadata
                        Material targetMat = configManager.getBukkitMaterial(progression.getMaterial());
                        if (item.getType() != targetMat) {
                            item.setType(targetMat);
                        }
                        toolService.applyMetadataToItem(item, progression);
                    }
                    updated = true;
                }
            }
            if (updated) {
                player.updateInventory();
                logger.info("[ToolItemSynchronizer] Reconciled physical item for player " + player.getName() + " [Tool UUID: " + progression.getToolUuid() + "]");
            }
        };

        if (Bukkit.isPrimaryThread()) {
            syncTask.run();
        } else if (plugin != null) {
            Bukkit.getScheduler().runTask(plugin, syncTask);
        }
    }

    /**
     * Reconciles a player's inventory on join against their authoritative DB progression state.
     *
     * @param player      joining player
     * @param progression authoritative DB record
     */
    public void reconcileOnJoin(Player player, ToolProgression progression) {
        synchronizePlayerTool(player, progression);
    }
}
