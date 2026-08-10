package io.github.kaivian.kupdater.features.tools.common.service;

import io.github.kaivian.kupdater.api.tools.model.RecoveryPreview;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryState;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.repository.ToolRecoveryRepository;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolDurabilityService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager.RecoveryConfig;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager.RecoveryItemRequirement;
import io.github.kaivian.kupdater.features.tools.common.economy.EconomyProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Deterministic calculation engine for evaluating recovery eligibility, progressive penalties, costs, and preview.
 */
public class RecoveryPenaltyEngine {

    private final ToolConfigManager configManager;
    private final ToolRepository toolRepository;
    private final ToolRecoveryRepository recoveryRepository;
    private final ToolDurabilityService durabilityService;
    private final EconomyProvider economyProvider;

    public RecoveryPenaltyEngine(ToolConfigManager configManager,
                                ToolRepository toolRepository,
                                ToolRecoveryRepository recoveryRepository,
                                ToolDurabilityService durabilityService,
                                EconomyProvider economyProvider) {
        this.configManager = configManager;
        this.toolRepository = toolRepository;
        this.recoveryRepository = recoveryRepository;
        this.durabilityService = durabilityService;
        this.economyProvider = economyProvider;
    }

    public RecoveryPreview createPreview(Player player, ToolProgression progression) {
        Instant now = Instant.now();
        RecoveryConfig cfg = configManager.getRecoveryConfig();

        if (cfg == null || !cfg.isEnabled()) {
            return new RecoveryPreview(RecoveryPreview.Status.DISABLED, player != null ? player.getUniqueId() : null,
                    progression, null, 0, 0, 0.0, 0, 0, 0.0, 0.0, false,
                    null, false, 0, 0, "Recovery system is disabled.");
        }

        if (progression == null) {
            return new RecoveryPreview(RecoveryPreview.Status.INVALID_TOOL_STATE, player != null ? player.getUniqueId() : null,
                    null, null, 0, 0, 0.0, 0, 0, 0.0, 0.0, false,
                    null, false, 0, 0, "No pickaxe progression record found.");
        }

        UUID ownerUuid = progression.getOwnerUuid();
        ToolState toolState = progression.getState();

        if (toolState == ToolState.DESTROYED) {
            return new RecoveryPreview(RecoveryPreview.Status.INVALID_TOOL_STATE, ownerUuid,
                    progression, null, 0, 0, 0.0, 0, 0, 0.0, 0.0, false,
                    null, false, 0, 0, cfg.getMsgBrokenCannotRecover());
        }

        if (toolState != ToolState.LOST) {
            return new RecoveryPreview(RecoveryPreview.Status.INVALID_TOOL_STATE, ownerUuid,
                    progression, null, 0, 0, 0.0, 0, 0, 0.0, 0.0, false,
                    null, false, 0, 0, cfg.getMsgNotLost());
        }

        ToolRecoveryState recState = recoveryRepository.findByToolUuid(progression.getToolUuid())
                .orElseGet(() -> ToolRecoveryState.initial(progression.getToolUuid(), ownerUuid));

        int storedCount = recState.getRecoveryCount(); // k
        int maxAllowed = cfg.getMaximumRecoveries();
        if (maxAllowed > 0 && storedCount >= maxAllowed) {
            return new RecoveryPreview(RecoveryPreview.Status.MAX_RECOVERIES_EXCEEDED, ownerUuid,
                    progression, recState, storedCount + 1, storedCount + 1, 0.0, 0, 0, 0.0, 0.0, false,
                    null, false, 0, 0, cfg.getMsgMaxRecoveries().replace("%max%", String.valueOf(maxAllowed)));
        }

        long remainingCd = recState.getRemainingCooldownSeconds(now);
        if (cfg.isCooldownEnabled() && remainingCd > 0) {
            return new RecoveryPreview(RecoveryPreview.Status.ON_COOLDOWN, ownerUuid,
                    progression, recState, storedCount + 1, storedCount + 1, 0.0, 0, 0, 0.0, 0.0, false,
                    null, false, 0, remainingCd, cfg.getMsgOnCooldown());
        }

        // 1. Durability Penalty
        int maxDur = configManager.getStat(progression.getMaterial(), progression.getLevel()).getMaxDurability();
        if (maxDur <= 0) {
            maxDur = 59; // Absolute fallback to vanilla wooden pickaxe
        }

        double durabilityPenaltyPct = 0.0;
        int resultingDur = maxDur;
        if (cfg.isDurabilityPenaltyEnabled()) {
            durabilityPenaltyPct = Math.min(cfg.getDurabilityMaximumPercent(),
                    cfg.getDurabilityBasePercent() + (storedCount * cfg.getDurabilityIncrementPercent()));
            durabilityPenaltyPct = Math.min(100.0, Math.max(0.0, durabilityPenaltyPct));
            resultingDur = Math.max(1, (int) Math.floor(maxDur * (1.0 - (durabilityPenaltyPct / 100.0))));
        }

        // 2. Currency Penalty
        double currencyCost = 0.0;
        if (cfg.isCurrencyEnabled()) {
            currencyCost = Math.min(cfg.getCurrencyMaximum(),
                    cfg.getCurrencyBase() + (storedCount * cfg.getCurrencyIncrement()));
            currencyCost = Math.max(0.0, currencyCost);
        }

        double currencyBalance = 0.0;
        boolean currencySufficient = true;
        boolean economyUnavailable = false;
        if (currencyCost > 0.0) {
            if (economyProvider == null || !economyProvider.isAvailable()) {
                economyUnavailable = true;
                currencySufficient = false;
            } else if (player != null) {
                currencyBalance = economyProvider.getBalance(player);
                currencySufficient = economyProvider.has(player, currencyCost);
            }
        }

        // 3. Item Requirements Penalty (Per-Tier)
        List<ItemStack> reqItems = new ArrayList<>();
        boolean hasItems = true;

        if (cfg.isItemsEnabled()) {
            String tierName = progression.getMaterial().name();
            java.util.List<RecoveryItemRequirement> tierReqs = cfg.getItemRequirementsForTier(tierName);

            if (tierReqs != null && !tierReqs.isEmpty()) {
                double mult = Math.min(cfg.getItemsMaximumMultiplier(),
                        cfg.getItemsBaseMultiplier() + (storedCount * cfg.getItemsIncrementMultiplier()));
                mult = Math.max(0.0, mult);

                for (RecoveryItemRequirement itemReq : tierReqs) {
                    int reqAmount = Math.max(1, (int) Math.floor(itemReq.getBaseAmount() * mult));
                    try {
                        ItemStack stack = new ItemStack(itemReq.getMaterial(), reqAmount);
                        reqItems.add(stack);
                    } catch (Throwable t) {
                        java.util.logging.Logger.getLogger("KUpdater-Recovery")
                                .warning("[Recovery] Failed to create ItemStack for " + itemReq.getMaterial() + " x" + reqAmount + ": " + t.getMessage());
                    }

                    if (player != null && !hasRequiredAmount(player, itemReq.getMaterial(), reqAmount)) {
                        hasItems = false;
                    }
                }
            }
        }

        // 4. Cooldown Penalty (calculated for next cooldown upon successful recovery)
        long cooldownDuration = 0L;
        if (cfg.isCooldownEnabled()) {
            cooldownDuration = Math.min(cfg.getCooldownMaximumSeconds(),
                    cfg.getCooldownBaseSeconds() + (storedCount * cfg.getCooldownIncrementSeconds()));
            cooldownDuration = Math.max(0L, cooldownDuration);
        }

        // Evaluate Status
        RecoveryPreview.Status status = RecoveryPreview.Status.AVAILABLE;
        String failReason = "";

        if (economyUnavailable) {
            status = RecoveryPreview.Status.ECONOMY_UNAVAILABLE;
            failReason = cfg.getMsgEconomyUnavailable();
        } else if (currencyCost > 0.0 && !currencySufficient) {
            status = RecoveryPreview.Status.INSUFFICIENT_FUNDS;
            failReason = cfg.getMsgInsufficientFunds()
                    .replace("%cost%", String.format("%.2f", currencyCost))
                    .replace("%balance%", String.format("%.2f", currencyBalance));
        } else if (!hasItems) {
            status = RecoveryPreview.Status.MISSING_ITEMS;
            failReason = cfg.getMsgMissingItems();
        } else if (player != null && !hasFreeSlot(player)) {
            status = RecoveryPreview.Status.INVENTORY_FULL;
            failReason = cfg.getMsgInventoryFull();
        }

        return new RecoveryPreview(
                status, ownerUuid, progression, recState, storedCount + 1, storedCount + 1,
                durabilityPenaltyPct, resultingDur, maxDur,
                currencyCost, currencyBalance, currencySufficient,
                reqItems, hasItems, cooldownDuration, 0L, failReason
        );
    }

    public boolean hasFreeSlot(Player player) {
        if (player == null || player.getInventory() == null) return true;
        ItemStack[] contents = player.getInventory().getContents();
        if (contents == null) return true;
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) return true;
        }
        return false;
    }

    public boolean hasRequiredAmount(Player player, Material material, int requiredAmount) {
        if (player == null || material == null || requiredAmount <= 0) return false;
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
                if (count >= requiredAmount) return true;
            }
        }
        return count >= requiredAmount;
    }
}
