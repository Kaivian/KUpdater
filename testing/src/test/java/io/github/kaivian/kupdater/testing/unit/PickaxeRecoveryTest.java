package io.github.kaivian.kupdater.testing.unit;

import io.github.kaivian.kupdater.api.tools.model.RecoveryPreview;
import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryHistoryEntry;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryState;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRecoveryRepository;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolDurabilityService;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.economy.EconomyProvider;
import io.github.kaivian.kupdater.features.tools.common.service.RecoveryPenaltyEngine;
import io.github.kaivian.kupdater.features.tools.common.service.RecoveryTransactionManager;
import io.github.kaivian.kupdater.features.tools.common.service.RecoveryTransactionManager.TransactionResult;
import io.github.kaivian.kupdater.features.tools.common.service.ToolRecoveryServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class PickaxeRecoveryTest {

    private MemoryToolRepository toolRepo;
    private MemoryToolRecoveryRepository recoveryRepo;
    private MockEconomyProvider economyProvider;
    private ToolConfigManager configManager;
    private ToolDurabilityService durabilityService;
    private MockToolService toolService;

    private RecoveryPenaltyEngine penaltyEngine;
    private RecoveryTransactionManager transactionManager;
    private ToolRecoveryServiceImpl recoveryService;

    private UUID ownerUuid;
    private UUID toolUuid;
    private ToolProgression lostProgression;
    private ToolProgression brokenProgression;
    private ToolProgression activeProgression;

    @BeforeEach
    void setUp() {
        ownerUuid = UUID.randomUUID();
        toolUuid = UUID.randomUUID();

        toolRepo = new MemoryToolRepository();
        recoveryRepo = new MemoryToolRecoveryRepository();
        economyProvider = new MockEconomyProvider();
        configManager = new ToolConfigManager(null);
        configManager.load(null);

        durabilityService = new ToolDurabilityService() {
            @Override
            public int getMaxDurability(String toolType, int level) {
                return 100;
            }

            @Override
            public ToolProgression applyDamage(org.bukkit.inventory.ItemStack itemStack, ToolProgression progression, int amount) {
                return progression;
            }

            @Override
            public boolean isExhausted(org.bukkit.inventory.ItemStack itemStack) {
                return false;
            }
        };

        toolService = new MockToolService();

        penaltyEngine = new RecoveryPenaltyEngine(configManager, toolRepo, recoveryRepo, durabilityService, economyProvider);
        transactionManager = new RecoveryTransactionManager(configManager, toolRepo, recoveryRepo, toolService, penaltyEngine, economyProvider, null);
        recoveryService = new ToolRecoveryServiceImpl(toolRepo, recoveryRepo, penaltyEngine, transactionManager);

        lostProgression = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, ToolMaterial.IRON, 3, 250, 50, ToolState.LOST, 0, 1, Instant.now(), Instant.now());
        brokenProgression = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, ToolMaterial.IRON, 3, 250, 50, ToolState.DESTROYED, 0, 1, Instant.now(), Instant.now());
        activeProgression = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, ToolMaterial.IRON, 3, 250, 50, ToolState.ACTIVE, 100, 1, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("State Eligibility: LOST can recover, BROKEN and ACTIVE are denied")
    void testStateEligibility() {
        toolRepo.save(lostProgression);
        assertTrue(recoveryService.isRecoverable(ownerUuid, ToolType.PICKAXE));

        RecoveryPreview previewLost = penaltyEngine.createPreview(null, lostProgression);
        assertEquals(RecoveryPreview.Status.AVAILABLE, previewLost.getStatus());

        RecoveryPreview previewBroken = penaltyEngine.createPreview(null, brokenProgression);
        assertEquals(RecoveryPreview.Status.INVALID_TOOL_STATE, previewBroken.getStatus());

        RecoveryPreview previewActive = penaltyEngine.createPreview(null, activeProgression);
        assertEquals(RecoveryPreview.Status.INVALID_TOOL_STATE, previewActive.getStatus());
    }

    @Test
    @DisplayName("Deterministic Penalty Scaling across multiple recoveries")
    void testPenaltyScaling() {
        toolRepo.save(lostProgression);

        // 1st Recovery (recoveryCount = 0 -> Rec #1)
        RecoveryPreview p1 = penaltyEngine.createPreview(null, lostProgression);
        assertEquals(1, p1.getRecoveryNumber());
        assertEquals(20.0, p1.getDurabilityPenaltyPercent());
        assertEquals(500.0, p1.getCurrencyCost());
        assertEquals(21600L, p1.getCooldownDurationSeconds());

        // Save state after 1st recovery (recoveryCount = 1)
        recoveryRepo.saveState(new ToolRecoveryState(toolUuid, ownerUuid, 1, Instant.now(), null, Instant.now(), Instant.now()));

        // 2nd Recovery (recoveryCount = 1 -> Rec #2)
        RecoveryPreview p2 = penaltyEngine.createPreview(null, lostProgression);
        assertEquals(2, p2.getRecoveryNumber());
        assertEquals(30.0, p2.getDurabilityPenaltyPercent());
        assertEquals(750.0, p2.getCurrencyCost());
        assertEquals(28800L, p2.getCooldownDurationSeconds());

        // 8th Recovery (max cap test)
        recoveryRepo.saveState(new ToolRecoveryState(toolUuid, ownerUuid, 7, Instant.now(), null, Instant.now(), Instant.now()));
        RecoveryPreview p8 = penaltyEngine.createPreview(null, lostProgression);
        assertEquals(80.0, p8.getDurabilityPenaltyPercent()); // Capped at max 80%
        assertEquals(2250.0, p8.getCurrencyCost());
        assertEquals(72000L, p8.getCooldownDurationSeconds());
    }

    @Test
    @DisplayName("Cooldown persistence and remaining cooldown calculation")
    void testCooldown() {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(3600); // 1 hour remaining

        ToolRecoveryState stateOnCd = new ToolRecoveryState(toolUuid, ownerUuid, 1, now, expires, now, now);
        recoveryRepo.saveState(stateOnCd);
        toolRepo.save(lostProgression);

        assertTrue(stateOnCd.isOnCooldown(now));
        assertTrue(stateOnCd.getRemainingCooldownSeconds(now) > 0);

        RecoveryPreview preview = penaltyEngine.createPreview(null, lostProgression);
        assertEquals(RecoveryPreview.Status.ON_COOLDOWN, preview.getStatus());
    }

    @Test
    @DisplayName("Progression Preservation on preview and restored state")
    void testProgressionPreservation() {
        toolRepo.save(lostProgression);
        RecoveryPreview preview = penaltyEngine.createPreview(null, lostProgression);

        assertEquals(lostProgression.getToolUuid(), preview.getProgression().getToolUuid());
        assertEquals(lostProgression.getOwnerUuid(), preview.getProgression().getOwnerUuid());
        assertEquals(lostProgression.getMaterial(), preview.getProgression().getMaterial());
        assertEquals(lostProgression.getLevel(), preview.getProgression().getLevel());
        assertEquals(lostProgression.getXp(), preview.getProgression().getXp());
        assertEquals(lostProgression.getOverflowXp(), preview.getProgression().getOverflowXp());
    }

    @Test
    @DisplayName("Recovery generates a new toolUuid and invalidates the old pickaxe")
    void testNewToolUuidOnRecovery() {
        toolRepo.save(lostProgression);
        org.bukkit.entity.Player player = createMockPlayer(ownerUuid);

        TransactionResult result = transactionManager.executeRecovery(player, true);
        assertTrue(result.isSuccess(), result.getMessage());

        ToolProgression recoveredProg = result.getRecoveredProgression();
        assertNotNull(recoveredProg);
        assertNotEquals(toolUuid, recoveredProg.getToolUuid()); // New UUID generated!
        assertEquals(ownerUuid, recoveredProg.getOwnerUuid());
        assertEquals(ToolState.ACTIVE, recoveredProg.getState());

        // Verify DB contains the new UUID for the player
        Optional<ToolProgression> dbProg = toolRepo.findByOwnerAndType(ownerUuid, ToolType.PICKAXE);
        assertTrue(dbProg.isPresent());
        assertEquals(recoveredProg.getToolUuid(), dbProg.get().getToolUuid());
    }

    // In-memory repositories and mock services for isolated unit testing
    private static class MemoryToolRepository implements ToolRepository {
        private final Map<UUID, ToolProgression> storage = new HashMap<>();

        @Override
        public Optional<ToolProgression> findById(UUID id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<ToolProgression> findByOwnerAndType(UUID ownerUuid, ToolType toolType) {
            return storage.values().stream()
                    .filter(p -> p.getOwnerUuid().equals(ownerUuid) && p.getToolType() == toolType)
                    .findFirst();
        }

        @Override
        public CompletableFuture<Optional<ToolProgression>> findByOwnerAndTypeAsync(UUID ownerUuid, ToolType toolType) {
            return CompletableFuture.completedFuture(findByOwnerAndType(ownerUuid, toolType));
        }

        @Override
        public boolean updateState(UUID toolUuid, ToolState newState) {
            ToolProgression existing = storage.get(toolUuid);
            if (existing != null) {
                storage.put(toolUuid, existing.withState(newState));
                return true;
            }
            return false;
        }

        @Override
        public boolean updateLevel(UUID toolUuid, int newLevel) {
            ToolProgression existing = storage.get(toolUuid);
            if (existing != null) {
                storage.put(toolUuid, existing.withLevel(newLevel));
                return true;
            }
            return false;
        }

        @Override
        public ToolProgression save(ToolProgression entity) {
            storage.entrySet().removeIf(e -> e.getValue().getOwnerUuid().equals(entity.getOwnerUuid()) && e.getValue().getToolType() == entity.getToolType());
            storage.put(entity.getToolUuid(), entity);
            return entity;
        }

        @Override
        public void deleteById(UUID id) {
            storage.remove(id);
        }

        @Override
        public List<ToolProgression> findAll() {
            return new ArrayList<>(storage.values());
        }
    }

    private static class MemoryToolRecoveryRepository implements ToolRecoveryRepository {
        private final Map<UUID, ToolRecoveryState> stateStorage = new HashMap<>();
        private final List<ToolRecoveryHistoryEntry> historyStorage = new ArrayList<>();

        @Override
        public Optional<ToolRecoveryState> findByToolUuid(UUID toolUuid) {
            return Optional.ofNullable(stateStorage.get(toolUuid));
        }

        @Override
        public CompletableFuture<Optional<ToolRecoveryState>> findByToolUuidAsync(UUID toolUuid) {
            return CompletableFuture.completedFuture(findByToolUuid(toolUuid));
        }

        @Override
        public Optional<ToolRecoveryState> findByOwnerUuid(UUID ownerUuid) {
            return stateStorage.values().stream()
                    .filter(s -> s.getOwnerUuid().equals(ownerUuid))
                    .findFirst();
        }

        @Override
        public ToolRecoveryState saveState(ToolRecoveryState state) {
            stateStorage.put(state.getToolUuid(), state);
            return state;
        }

        @Override
        public ToolRecoveryHistoryEntry saveHistory(ToolRecoveryHistoryEntry entry) {
            historyStorage.add(entry);
            return entry;
        }

        @Override
        public List<ToolRecoveryHistoryEntry> findHistoryByToolUuid(UUID toolUuid) {
            List<ToolRecoveryHistoryEntry> list = new ArrayList<>();
            for (ToolRecoveryHistoryEntry e : historyStorage) {
                if (e.getToolUuid().equals(toolUuid)) list.add(e);
            }
            return list;
        }
    }

    private static class MockEconomyProvider implements EconomyProvider {
        private final Map<UUID, Double> balances = new HashMap<>();
        private boolean available = true;

        public void setAvailable(boolean available) { this.available = available; }
        public void setBalance(UUID player, double balance) { balances.put(player, balance); }

        @Override
        public boolean isAvailable() { return available; }

        @Override
        public double getBalance(org.bukkit.entity.Player player) {
            return player != null ? balances.getOrDefault(player.getUniqueId(), 10000.0) : 0.0;
        }

        @Override
        public boolean has(org.bukkit.entity.Player player, double amount) {
            return getBalance(player) >= amount;
        }

        @Override
        public boolean withdraw(org.bukkit.entity.Player player, double amount) {
            if (has(player, amount)) {
                balances.put(player.getUniqueId(), getBalance(player) - amount);
                return true;
            }
            return false;
        }

        @Override
        public boolean deposit(org.bukkit.entity.Player player, double amount) {
            if (player != null) {
                balances.put(player.getUniqueId(), getBalance(player) + amount);
                return true;
            }
            return false;
        }
    }

    private static class MockToolService implements ToolService {
        @Override
        public Optional<ToolProgression> getProgression(UUID ownerUuid, ToolType toolType) { return Optional.empty(); }

        @Override
        public Optional<ToolProgression> getProgressionByToolUuid(UUID toolUuid) { return Optional.empty(); }

        @Override
        public Optional<ToolProgression> getProgressionFromItem(org.bukkit.inventory.ItemStack itemStack) { return Optional.empty(); }

        @Override
        public Optional<ToolProgression> registerInitialTool(org.bukkit.entity.Player player, org.bukkit.inventory.ItemStack itemStack) { return Optional.empty(); }

        @Override
        public void applyMetadataToItem(org.bukkit.inventory.ItemStack itemStack, ToolProgression progression) {}

        @Override
        public boolean updateState(UUID toolUuid, ToolState newState) { return true; }

        @Override
        public boolean isManagedTool(org.bukkit.inventory.ItemStack itemStack) { return false; }

        @Override
        public Optional<UUID> getToolUuidFromItem(org.bukkit.inventory.ItemStack itemStack) { return Optional.empty(); }
    }

    private static org.bukkit.entity.Player createMockPlayer(UUID uuid) {
        org.bukkit.inventory.PlayerInventory mockInv = org.mockito.Mockito.mock(org.bukkit.inventory.PlayerInventory.class);
        org.mockito.Mockito.when(mockInv.getContents()).thenReturn(new org.bukkit.inventory.ItemStack[36]);
        org.mockito.Mockito.when(mockInv.addItem(org.mockito.Mockito.any())).thenReturn(new HashMap<>());

        return (org.bukkit.entity.Player) java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.entity.Player.class.getClassLoader(),
                new Class<?>[]{org.bukkit.entity.Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return uuid;
                    if ("getName".equals(method.getName())) return "TestPlayer";
                    if ("getInventory".equals(method.getName())) return mockInv;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    return null;
                }
        );
    }
}
