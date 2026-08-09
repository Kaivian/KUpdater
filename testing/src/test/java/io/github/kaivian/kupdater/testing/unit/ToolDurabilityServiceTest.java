package io.github.kaivian.kupdater.testing.unit;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolDurabilityService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.metadata.ToolItemMetadataService;
import io.github.kaivian.kupdater.features.tools.common.service.ToolDurabilityServiceImpl;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ToolDurabilityServiceTest {

    private ToolConfigManager configManager;
    private FakeToolRepository repository;
    private ToolDurabilityService durabilityService;

    private UUID toolUuid;
    private UUID ownerUuid;

    @BeforeEach
    void setUp() {
        configManager = new ToolConfigManager(null);
        configManager.load(null);
        repository = new FakeToolRepository();
        durabilityService = new ToolDurabilityServiceImpl(configManager, repository, new ToolItemMetadataService(null), null);

        toolUuid = UUID.randomUUID();
        ownerUuid = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should reduce logical durability upon damage while keeping tool ACTIVE")
    void testNormalDamageApplication() {
        ToolProgression active = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, 1, ToolState.ACTIVE, 59, 1, Instant.now(), Instant.now());

        ToolProgression updated = durabilityService.applyDamage(null, active, 10);

        assertNotNull(updated);
        assertEquals(49, updated.getCurrentDurability());
        assertEquals(ToolState.ACTIVE, updated.getState());
    }

    @Test
    @DisplayName("Should transition state to DESTROYED and retain physical ItemStack when durability is exhausted")
    void testDurabilityExhaustionRetention() {
        ToolProgression lowDurability = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, 1, ToolState.ACTIVE, 5, 1, Instant.now(), Instant.now());

        ToolProgression destroyed = durabilityService.applyDamage(null, lowDurability, 10);

        assertNotNull(destroyed);
        assertEquals(0, destroyed.getCurrentDurability());
        assertEquals(ToolState.DESTROYED, destroyed.getState());

        Optional<ToolProgression> stored = repository.findById(toolUuid);
        assertTrue(stored.isPresent());
        assertEquals(ToolState.DESTROYED, stored.get().getState());
    }

    private static class FakeToolRepository implements ToolRepository {
        private ToolProgression saved;

        @Override public Optional<ToolProgression> findById(UUID id) { return Optional.ofNullable(saved); }
        @Override public List<ToolProgression> findAll() { return new ArrayList<>(); }
        @Override public ToolProgression save(ToolProgression entity) { this.saved = entity; return entity; }
        @Override public void deleteById(UUID id) {}
        @Override public Optional<ToolProgression> findByOwnerAndType(UUID ownerUuid, ToolType toolType) { return Optional.ofNullable(saved); }
        @Override public CompletableFuture<Optional<ToolProgression>> findByOwnerAndTypeAsync(UUID ownerUuid, ToolType toolType) { return CompletableFuture.completedFuture(Optional.ofNullable(saved)); }
        @Override public boolean updateState(UUID toolUuid, ToolState newState) { return true; }
        @Override public boolean updateLevel(UUID toolUuid, int newLevel) { return true; }
    }
}
