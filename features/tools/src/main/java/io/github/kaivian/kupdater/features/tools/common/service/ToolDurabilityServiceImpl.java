package io.github.kaivian.kupdater.features.tools.common.service;

import io.github.kaivian.kupdater.api.tools.event.ToolStateChangeEvent;
import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolDurabilityService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.metadata.ToolItemMetadataService;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Implementation of ToolDurabilityService for version-independent logical durability handling.
 */
public class ToolDurabilityServiceImpl implements ToolDurabilityService {

    private final ToolConfigManager configManager;
    private final ToolRepository repository;
    private final ToolItemMetadataService metadataService;
    private final Logger logger;

    public ToolDurabilityServiceImpl(ToolConfigManager configManager, ToolRepository repository,
                                      ToolItemMetadataService metadataService, Logger logger) {
        this.configManager = configManager;
        this.repository = repository;
        this.metadataService = metadataService;
        this.logger = logger != null ? logger : Logger.getLogger("ToolDurabilityService");
    }

    @Override
    public int getMaxDurability(String toolType, int level) {
        return configManager.getStat(ToolMaterial.WOODEN, level).getMaxDurability();
    }

    public int getMaxDurability(ToolMaterial material, int level) {
        return configManager.getStat(material, level).getMaxDurability();
    }

    @Override
    public ToolProgression applyDamage(ItemStack itemStack, ToolProgression progression, int amount) {
        if (progression == null) return null;

        int current = progression.getCurrentDurability();
        int newDurability = Math.max(0, current - Math.max(1, amount));
        int maxDurability = getMaxDurability(progression.getMaterial(), progression.getLevel());

        if (newDurability == 0) {
            ToolState oldState = progression.getState();
            ToolProgression destroyedProgression = progression
                    .withDurability(0)
                    .withState(ToolState.DESTROYED);

            ToolProgression saved = repository.save(destroyedProgression);

            // Stamp PDC metadata & lore onto physical item so it remains in inventory as broken/unusable tool
            if (itemStack != null) {
                io.github.kaivian.kupdater.api.tools.model.ToolStat stat = configManager.getStat(saved.getMaterial(), saved.getLevel());
                metadataService.stampMetadata(itemStack, saved, maxDurability, stat.getRequiredXp(), stat.getMiningSpeedMultiplier());
            }

            logger.info("[ToolUpdater] Pickaxe " + saved.getToolUuid() + " entered DESTROYED state.");

            if (Bukkit.getServer() != null && Bukkit.getPluginManager() != null) {
                Bukkit.getPluginManager().callEvent(new ToolStateChangeEvent(saved, oldState, ToolState.DESTROYED));
            }

            return saved;
        } else {
            ToolProgression updated = progression.withDurability(newDurability);
            ToolProgression saved = repository.save(updated);

            if (itemStack != null) {
                io.github.kaivian.kupdater.api.tools.model.ToolStat stat = configManager.getStat(saved.getMaterial(), saved.getLevel());
                metadataService.stampMetadata(itemStack, saved, maxDurability, stat.getRequiredXp(), stat.getMiningSpeedMultiplier());
            }

            return saved;
        }
    }

    @Override
    public boolean isExhausted(ItemStack itemStack) {
        if (itemStack == null || !metadataService.isManagedTool(itemStack)) return false;
        Optional<UUID> toolUuidOpt = metadataService.getToolUuid(itemStack);
        if (!toolUuidOpt.isPresent()) return false;

        Optional<ToolProgression> progOpt = repository.findById(toolUuidOpt.get());
        if (!progOpt.isPresent()) return false;

        ToolProgression progression = progOpt.get();
        return progression.getState() == ToolState.DESTROYED || progression.getCurrentDurability() <= 0;
    }
}
