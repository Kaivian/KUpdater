package io.github.kaivian.kupdater.features.tools.common.service;

import io.github.kaivian.kupdater.api.tools.event.ToolRegisterEvent;
import io.github.kaivian.kupdater.api.tools.event.ToolStateChangeEvent;
import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolDurabilityService;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.metadata.ToolItemMetadataService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Primary implementation of ToolService.
 */
public class ToolServiceImpl implements ToolService {

    private final ToolRepository repository;
    private final ToolItemMetadataService metadataService;
    private final ToolConfigManager configManager;
    private final ToolDurabilityService durabilityService;
    private final Logger logger;

    public ToolServiceImpl(ToolRepository repository, ToolItemMetadataService metadataService,
                           ToolConfigManager configManager, ToolDurabilityService durabilityService, Logger logger) {
        this.repository = repository;
        this.metadataService = metadataService;
        this.configManager = configManager;
        this.durabilityService = durabilityService;
        this.logger = logger != null ? logger : Logger.getLogger("ToolService");
    }

    @Override
    public Optional<ToolProgression> getProgression(UUID ownerUuid, ToolType toolType) {
        if (ownerUuid == null || toolType == null) return Optional.empty();
        return repository.findByOwnerAndType(ownerUuid, toolType);
    }

    @Override
    public Optional<ToolProgression> getProgressionByToolUuid(UUID toolUuid) {
        if (toolUuid == null) return Optional.empty();
        return repository.findById(toolUuid);
    }

    @Override
    public Optional<ToolProgression> getProgressionFromItem(ItemStack itemStack) {
        Optional<UUID> toolUuidOpt = metadataService.getToolUuid(itemStack);
        if (!toolUuidOpt.isPresent()) {
            return Optional.empty();
        }

        UUID toolUuid = toolUuidOpt.get();
        Optional<ToolProgression> dbRecord = repository.findById(toolUuid);
        if (!dbRecord.isPresent()) {
            return Optional.empty();
        }

        ToolProgression progression = dbRecord.get();

        // Validate cached PDC owner vs DB authoritative owner
        Optional<UUID> pdcOwnerOpt = metadataService.getOwnerUuid(itemStack);
        if (pdcOwnerOpt.isPresent() && !pdcOwnerOpt.get().equals(progression.getOwnerUuid())) {
            logger.warning("[ToolService] PDC owner UUID (" + pdcOwnerOpt.get()
                    + ") mismatches authoritative DB owner UUID (" + progression.getOwnerUuid()
                    + ") for tool " + toolUuid + ". Database takes precedence.");
        }

        return Optional.of(progression);
    }

    private boolean isPickaxe(Material material) {
        if (material == null) return false;
        return material.name().endsWith("_PICKAXE");
    }

    @Override
    public Optional<ToolProgression> registerInitialTool(Player player, ItemStack itemStack) {
        if (player == null) return Optional.empty();
        if (itemStack != null && !isPickaxe(itemStack.getType())) return Optional.empty();

        ToolType toolType = ToolType.PICKAXE;
        ToolMaterial initialMaterial = itemStack != null ?
                metadataService.getMaterial(itemStack).orElseGet(() -> ToolMaterial.fromBukkitMaterial(itemStack.getType())) :
                ToolMaterial.WOODEN;

        Optional<ToolProgression> existing = repository.findByOwnerAndType(player.getUniqueId(), toolType);
        if (existing.isPresent()) {
            ToolProgression prog = existing.get();
            if (prog.getState() == ToolState.ACTIVE && itemStack != null && isPickaxe(itemStack.getType())) {
                ToolMaterial itemMat = metadataService.getMaterial(itemStack)
                        .orElseGet(() -> ToolMaterial.fromBukkitMaterial(itemStack.getType()));
                if (itemMat.ordinal() > prog.getMaterial().ordinal()) {
                    io.github.kaivian.kupdater.api.tools.model.ToolStat newMatStat = configManager.getStat(itemMat, 1);
                    prog = prog.withMaterialAndLevel(itemMat, 1, newMatStat.getMaxDurability()).withXp(0);
                    prog = repository.save(prog);
                }
                applyMetadataToItem(itemStack, prog);
                return Optional.of(prog);
            }
            return Optional.empty();
        }

        io.github.kaivian.kupdater.api.tools.model.ToolStat stat = configManager.getStat(initialMaterial, 1);
        UUID toolUuid = UUID.randomUUID();
        ToolProgression newProgression = new ToolProgression(
                toolUuid,
                player.getUniqueId(),
                toolType,
                initialMaterial,
                1,
                0,
                ToolState.ACTIVE,
                stat.getMaxDurability(),
                1,
                Instant.now(),
                Instant.now()
        );

        ToolProgression saved = repository.save(newProgression);
        if (itemStack != null && isPickaxe(itemStack.getType())) {
            applyMetadataToItem(itemStack, saved);
        }

        logger.info("[ToolUpdater] Registered initial Pickaxe " + toolUuid + " for player " + player.getName() + " (" + player.getUniqueId() + ")");

        if (Bukkit.getServer() != null && Bukkit.getPluginManager() != null) {
            Bukkit.getPluginManager().callEvent(new ToolRegisterEvent(player, saved));
        }

        return Optional.of(saved);
    }

    @Override
    public void applyMetadataToItem(ItemStack itemStack, ToolProgression progression) {
        if (itemStack == null || progression == null) return;

        // Synchronize Bukkit Material to match the progression's ToolMaterial
        org.bukkit.Material expectedMaterial = configManager.getBukkitMaterial(progression.getMaterial());
        if (expectedMaterial != null && itemStack.getType() != expectedMaterial) {
            itemStack.setType(expectedMaterial);
        }

        io.github.kaivian.kupdater.api.tools.model.ToolStat stat = configManager.getStat(progression.getMaterial(), progression.getLevel());
        metadataService.stampMetadata(itemStack, progression, stat.getMaxDurability(), stat.getRequiredXp(), stat.getMiningSpeedMultiplier());
    }

    @Override
    public boolean updateState(UUID toolUuid, ToolState newState) {
        if (toolUuid == null || newState == null) return false;

        Optional<ToolProgression> currentOpt = repository.findById(toolUuid);
        if (!currentOpt.isPresent()) return false;

        ToolProgression current = currentOpt.get();
        if (current.getState() == newState) return true;

        boolean success = repository.updateState(toolUuid, newState);
        if (success && Bukkit.getServer() != null && Bukkit.getPluginManager() != null) {
            ToolProgression updated = current.withState(newState);
            Bukkit.getPluginManager().callEvent(new ToolStateChangeEvent(updated, current.getState(), newState));
            logger.info("[ToolUpdater] Pickaxe " + toolUuid + " transitioned state: " + current.getState() + " -> " + newState);
        }
        return success;
    }

    @Override
    public boolean isManagedTool(ItemStack itemStack) {
        return metadataService.isManagedTool(itemStack);
    }

    @Override
    public Optional<UUID> getToolUuidFromItem(ItemStack itemStack) {
        return metadataService.getToolUuid(itemStack);
    }
}
