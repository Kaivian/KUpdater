package io.github.kaivian.kupdater.features.tools.common.service;

import io.github.kaivian.kupdater.api.tools.model.RecoveryPreview;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryHistoryEntry;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryState;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.repository.ToolRecoveryRepository;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager.RecoveryConfig;
import io.github.kaivian.kupdater.features.tools.common.economy.EconomyProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes transaction-safe pickaxe recovery operations with per-player concurrency locks,
 * resource deduction, compensation rollback, and audit logging.
 */
public class RecoveryTransactionManager {

    public enum ResultType {
        SUCCESS,
        REJECTED_NOT_LOST,
        REJECTED_BROKEN,
        REJECTED_COOLDOWN,
        REJECTED_INSUFFICIENT_FUNDS,
        REJECTED_MISSING_ITEMS,
        REJECTED_INVENTORY_FULL,
        REJECTED_ECONOMY_UNAVAILABLE,
        REJECTED_CONCURRENT_TRANSACTION,
        REJECTED_MAX_RECOVERIES,
        FAILED_SYSTEM_ERROR
    }

    public static final class TransactionResult {
        private final ResultType type;
        private final String message;
        private final ToolProgression recoveredProgression;
        private final ItemStack recoveredItem;

        public TransactionResult(ResultType type, String message, ToolProgression recoveredProgression, ItemStack recoveredItem) {
            this.type = Objects.requireNonNull(type, "type cannot be null");
            this.message = message != null ? message : "";
            this.recoveredProgression = recoveredProgression;
            this.recoveredItem = recoveredItem;
        }

        public ResultType getType() { return type; }
        public boolean isSuccess() { return type == ResultType.SUCCESS; }
        public String getMessage() { return message; }
        public ToolProgression getRecoveredProgression() { return recoveredProgression; }
        public ItemStack getRecoveredItem() { return recoveredItem; }
    }

    private final ToolConfigManager configManager;
    private final ToolRepository toolRepository;
    private final ToolRecoveryRepository recoveryRepository;
    private final ToolService toolService;
    private final RecoveryPenaltyEngine penaltyEngine;
    private final EconomyProvider economyProvider;
    private final Logger logger;

    private final ConcurrentHashMap<UUID, AtomicBoolean> activeLocks = new ConcurrentHashMap<>();

    public RecoveryTransactionManager(ToolConfigManager configManager,
                                      ToolRepository toolRepository,
                                      ToolRecoveryRepository recoveryRepository,
                                      ToolService toolService,
                                      RecoveryPenaltyEngine penaltyEngine,
                                      EconomyProvider economyProvider,
                                      Logger logger) {
        this.configManager = configManager;
        this.toolRepository = toolRepository;
        this.recoveryRepository = recoveryRepository;
        this.toolService = toolService;
        this.penaltyEngine = penaltyEngine;
        this.economyProvider = economyProvider;
        this.logger = logger != null ? logger : Logger.getLogger("RecoveryTransactionManager");
    }

    public TransactionResult executeRecovery(Player player, boolean adminForce) {
        if (player == null) {
            return new TransactionResult(ResultType.FAILED_SYSTEM_ERROR, "Player is offline.", null, null);
        }

        UUID playerUuid = player.getUniqueId();
        AtomicBoolean lock = activeLocks.computeIfAbsent(playerUuid, u -> new AtomicBoolean(false));

        if (!lock.compareAndSet(false, true)) {
            RecoveryConfig cfg = configManager.getRecoveryConfig();
            String msg = cfg != null ? cfg.getMsgAlreadyProcessing() : "Recovery transaction is already processing.";
            return new TransactionResult(ResultType.REJECTED_CONCURRENT_TRANSACTION, msg, null, null);
        }

        try {
            return processTransaction(player, adminForce);
        } finally {
            lock.set(false);
        }
    }

    private TransactionResult processTransaction(Player player, boolean adminForce) {
        Optional<ToolProgression> progOpt = toolRepository.findByOwnerAndType(player.getUniqueId(), io.github.kaivian.kupdater.api.tools.model.ToolType.PICKAXE);
        if (!progOpt.isPresent()) {
            return new TransactionResult(ResultType.REJECTED_NOT_LOST, "No pickaxe progression found for player.", null, null);
        }

        ToolProgression progression = progOpt.get();

        // Server-side fresh revalidation
        RecoveryPreview preview = penaltyEngine.createPreview(player, progression);

        if (!adminForce && !preview.isAvailable()) {
            switch (preview.getStatus()) {
                case ON_COOLDOWN:
                    return new TransactionResult(ResultType.REJECTED_COOLDOWN, preview.getFailureReason(), null, null);
                case INSUFFICIENT_FUNDS:
                    return new TransactionResult(ResultType.REJECTED_INSUFFICIENT_FUNDS, preview.getFailureReason(), null, null);
                case MISSING_ITEMS:
                    return new TransactionResult(ResultType.REJECTED_MISSING_ITEMS, preview.getFailureReason(), null, null);
                case INVENTORY_FULL:
                    return new TransactionResult(ResultType.REJECTED_INVENTORY_FULL, preview.getFailureReason(), null, null);
                case ECONOMY_UNAVAILABLE:
                    return new TransactionResult(ResultType.REJECTED_ECONOMY_UNAVAILABLE, preview.getFailureReason(), null, null);
                case MAX_RECOVERIES_EXCEEDED:
                    return new TransactionResult(ResultType.REJECTED_MAX_RECOVERIES, preview.getFailureReason(), null, null);
                case INVALID_TOOL_STATE:
                    if (progression.getState() == ToolState.DESTROYED) {
                        return new TransactionResult(ResultType.REJECTED_BROKEN, preview.getFailureReason(), null, null);
                    } else {
                        return new TransactionResult(ResultType.REJECTED_NOT_LOST, preview.getFailureReason(), null, null);
                    }
                default:
                    return new TransactionResult(ResultType.FAILED_SYSTEM_ERROR, preview.getFailureReason(), null, null);
            }
        }

        // Strict Inventory Space Check
        if (!penaltyEngine.hasFreeSlot(player)) {
            RecoveryConfig cfg = configManager.getRecoveryConfig();
            String msg = cfg != null ? cfg.getMsgInventoryFull() : "You need at least 1 free inventory slot.";
            return new TransactionResult(ResultType.REJECTED_INVENTORY_FULL, msg, null, null);
        }

        // Step 1: Deduct Currency & Items (Tracked for potential compensation rollback)
        boolean currencyDeducted = false;
        double costDeducted = 0.0;
        List<ItemStack> itemsDeducted = new ArrayList<>();

        if (!adminForce && preview.getCurrencyCost() > 0.0) {
            costDeducted = preview.getCurrencyCost();
            if (economyProvider != null && economyProvider.withdraw(player, costDeducted)) {
                currencyDeducted = true;
            } else {
                return new TransactionResult(ResultType.REJECTED_INSUFFICIENT_FUNDS, "Economy transaction failed.", null, null);
            }
        }

        if (!adminForce && preview.getRequiredItems() != null && !preview.getRequiredItems().isEmpty()) {
            for (ItemStack reqStack : preview.getRequiredItems()) {
                if (consumeItemFromInventory(player, reqStack.getType(), reqStack.getAmount())) {
                    itemsDeducted.add(new ItemStack(reqStack.getType(), reqStack.getAmount()));
                } else {
                    // Partial deduction failure -> Compensation rollback
                    rollbackDeductions(player, currencyDeducted, costDeducted, itemsDeducted);
                    return new TransactionResult(ResultType.REJECTED_MISSING_ITEMS, "Failed to consume required items from inventory.", null, null);
                }
            }
        }

        // Step 2: Commit DB State Transition & Prepare Restored Pickaxe
        try {
            UUID oldToolUuid = progression.getToolUuid();
            UUID newToolUuid = UUID.randomUUID();

            ToolProgression restoredProgression = progression
                    .withToolUuid(newToolUuid)
                    .withState(ToolState.ACTIVE)
                    .withDurability(preview.getResultingDurability());

            ToolProgression savedProgression = toolRepository.save(restoredProgression);
            if (!oldToolUuid.equals(newToolUuid)) {
                toolRepository.deleteById(oldToolUuid);
            }

            Instant now = Instant.now();
            Instant cdExpiresAt = preview.getCooldownDurationSeconds() > 0 ? now.plusSeconds(preview.getCooldownDurationSeconds()) : null;

            ToolRecoveryState currentRecState = recoveryRepository.findByToolUuid(oldToolUuid)
                    .orElseGet(() -> recoveryRepository.findByOwnerUuid(player.getUniqueId())
                            .orElseGet(() -> ToolRecoveryState.initial(newToolUuid, player.getUniqueId())));

            ToolRecoveryState updatedRecState = currentRecState.withToolUuid(newToolUuid).withCompletedRecovery(now, cdExpiresAt);
            recoveryRepository.saveState(updatedRecState);

            // Step 3: Instantiate and deliver restored physical pickaxe ItemStack
            ItemStack restoredStack;
            if (org.bukkit.Bukkit.getServer() != null) {
                Material bMat = configManager.getBukkitMaterial(savedProgression.getMaterial());
                restoredStack = new ItemStack(bMat, 1);
                toolService.applyMetadataToItem(restoredStack, savedProgression);

                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(restoredStack);
                if (!leftover.isEmpty()) {
                    logger.severe("[RecoveryTransactionManager] Failed to place pickaxe in inventory for player " + player.getName() + ". Initiating rollback.");
                    toolRepository.save(progression); // restore DB state to LOST
                    recoveryRepository.saveState(currentRecState);
                    rollbackDeductions(player, currencyDeducted, costDeducted, itemsDeducted);
                    return new TransactionResult(ResultType.FAILED_SYSTEM_ERROR, "Failed to place restored pickaxe into inventory.", null, null);
                }
            } else {
                restoredStack = null;
            }

            // Step 5: Save structured history entry
            String itemsJson = buildItemsConsumedJson(itemsDeducted);
            ToolRecoveryHistoryEntry historyEntry = ToolRecoveryHistoryEntry.create(
                    savedProgression.getToolUuid(),
                    player.getUniqueId(),
                    updatedRecState.getRecoveryCount(),
                    preview.getPenaltyLevel(),
                    costDeducted,
                    itemsJson,
                    preview.getDurabilityPenaltyPercent()
            );
            recoveryRepository.saveHistory(historyEntry);

            logger.info("[RecoveryTransactionManager] Successfully recovered Pickaxe " + savedProgression.getToolUuid()
                    + " for player " + player.getName() + " (Recovery #" + updatedRecState.getRecoveryCount() + ")");

            return new TransactionResult(ResultType.SUCCESS, "Pickaxe recovered successfully!", savedProgression, restoredStack);

        } catch (Throwable t) {
            t.printStackTrace();
            logger.log(Level.SEVERE, "[RecoveryTransactionManager] Unexpected error during recovery execution for " + player.getName(), t);
            rollbackDeductions(player, currencyDeducted, costDeducted, itemsDeducted);
            return new TransactionResult(ResultType.FAILED_SYSTEM_ERROR, "System error during recovery execution: " + t.getMessage(), null, null);
        }
    }

    private boolean consumeItemFromInventory(Player player, Material material, int amountToConsume) {
        if (player == null || material == null || amountToConsume <= 0) return true;
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amountToConsume;

        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == material) {
                int count = stack.getAmount();
                if (count <= remaining) {
                    remaining -= count;
                    player.getInventory().setItem(i, null);
                } else {
                    stack.setAmount(count - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }
        return remaining <= 0;
    }

    private void rollbackDeductions(Player player, boolean currencyDeducted, double costDeducted, List<ItemStack> itemsDeducted) {
        if (player == null) return;
        if (currencyDeducted && costDeducted > 0.0 && economyProvider != null) {
            try {
                economyProvider.deposit(player, costDeducted);
                logger.info("[RecoveryTransactionManager] Refunded $" + costDeducted + " to " + player.getName() + " due to recovery rollback.");
            } catch (Exception e) {
                logger.severe("[RecoveryTransactionManager] Failed to refund currency to " + player.getName() + ": " + e.getMessage());
            }
        }
        if (itemsDeducted != null && !itemsDeducted.isEmpty()) {
            for (ItemStack item : itemsDeducted) {
                try {
                    player.getInventory().addItem(item);
                } catch (Exception e) {
                    logger.severe("[RecoveryTransactionManager] Failed to return item " + item.getType() + " to " + player.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private String buildItemsConsumedJson(List<ItemStack> items) {
        if (items == null || items.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"material\":\"").append(s.getType().name()).append("\",\"amount\":").append(s.getAmount()).append("}");
        }
        sb.append("]");
        return sb.toString();
    }
}
