package io.github.kaivian.kupdater.testing.unit;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.metadata.ToolItemMetadataService;
import io.github.kaivian.kupdater.features.tools.common.service.ToolServiceImpl;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Tag("unit")
class ProgressionBypassTest {

    private FakeToolRepository repository;
    private ToolConfigManager configManager;
    private ToolService toolService;

    private Player player;
    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        repository = new FakeToolRepository();
        configManager = new ToolConfigManager(null);
        configManager.load(null);
        toolService = new ToolServiceImpl(repository, new ToolItemMetadataService(null), configManager, null, null);

        player = Mockito.mock(Player.class);
        playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
    }

    @Test
    @DisplayName("Should register initial Pickaxe when player has no existing progression")
    void testRegisterInitialToolWhenNoProgressionExists() {
        Optional<ToolProgression> registeredOpt = toolService.registerInitialTool(player, null);

        assertTrue(registeredOpt.isPresent());
        assertEquals(1, registeredOpt.get().getLevel());
        assertEquals(ToolState.ACTIVE, registeredOpt.get().getState());
    }

    @Test
    @DisplayName("Should PREVENT second progression when ACTIVE Pickaxe exists")
    void testBypassPreventionWhenActiveExists() {
        ToolProgression active = new ToolProgression(UUID.randomUUID(), playerUuid, ToolType.PICKAXE, 3, ToolState.ACTIVE, 95, 1, Instant.now(), Instant.now());
        repository.setExisting(active);

        Optional<ToolProgression> registeredOpt = toolService.registerInitialTool(player, null);

        assertFalse(registeredOpt.isPresent());
    }

    @Test
    @DisplayName("Should PREVENT second progression when LOST Pickaxe exists")
    void testBypassPreventionWhenLostExists() {
        ToolProgression lost = new ToolProgression(UUID.randomUUID(), playerUuid, ToolType.PICKAXE, 3, ToolState.LOST, 95, 1, Instant.now(), Instant.now());
        repository.setExisting(lost);

        Optional<ToolProgression> registeredOpt = toolService.registerInitialTool(player, null);

        assertFalse(registeredOpt.isPresent());
    }

    @Test
    @DisplayName("Should PREVENT second progression when DESTROYED Pickaxe exists")
    void testBypassPreventionWhenDestroyedExists() {
        ToolProgression destroyed = new ToolProgression(UUID.randomUUID(), playerUuid, ToolType.PICKAXE, 3, ToolState.DESTROYED, 0, 1, Instant.now(), Instant.now());
        repository.setExisting(destroyed);

        Optional<ToolProgression> registeredOpt = toolService.registerInitialTool(player, null);

        assertFalse(registeredOpt.isPresent());
    }

    private static class FakeToolRepository implements ToolRepository {
        private ToolProgression existing;

        public void setExisting(ToolProgression existing) { this.existing = existing; }

        @Override public Optional<ToolProgression> findById(UUID id) { return Optional.ofNullable(existing); }
        @Override public List<ToolProgression> findAll() { return new ArrayList<>(); }
        @Override public ToolProgression save(ToolProgression entity) { this.existing = entity; return entity; }
        @Override public void deleteById(UUID id) {}
        @Override public Optional<ToolProgression> findByOwnerAndType(UUID ownerUuid, ToolType toolType) { return Optional.ofNullable(existing); }
        @Override public CompletableFuture<Optional<ToolProgression>> findByOwnerAndTypeAsync(UUID ownerUuid, ToolType toolType) { return CompletableFuture.completedFuture(Optional.ofNullable(existing)); }
        @Override public boolean updateState(UUID toolUuid, ToolState newState) { return true; }
        @Override public boolean updateLevel(UUID toolUuid, int newLevel) { return true; }
    }
}
