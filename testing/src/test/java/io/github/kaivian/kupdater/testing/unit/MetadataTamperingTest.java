package io.github.kaivian.kupdater.testing.unit;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.metadata.ToolItemMetadataService;
import io.github.kaivian.kupdater.features.tools.common.service.ToolServiceImpl;
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
class MetadataTamperingTest {

    private FakeToolRepository repository;
    private FakeMetadataService metadataService;
    private ToolConfigManager configManager;
    private ToolService toolService;

    private UUID toolUuid;
    private UUID dbOwnerUuid;
    private UUID tamperedOwnerUuid;

    @BeforeEach
    void setUp() {
        repository = new FakeToolRepository();
        metadataService = new FakeMetadataService();
        configManager = new ToolConfigManager(null);
        configManager.load(null);
        toolService = new ToolServiceImpl(repository, metadataService, configManager, null, null);

        toolUuid = UUID.randomUUID();
        dbOwnerUuid = UUID.randomUUID();
        tamperedOwnerUuid = UUID.randomUUID();

        metadataService.setToolUuid(toolUuid);
        metadataService.setOwnerUuid(tamperedOwnerUuid);
    }

    @Test
    @DisplayName("Database state must override tampered PDC metadata")
    void testDatabaseOverridesTamperedMetadata() {
        ToolProgression dbProgression = new ToolProgression(toolUuid, dbOwnerUuid, ToolType.PICKAXE, 2, ToolState.ACTIVE, 75, 1, Instant.now(), Instant.now());
        repository.setExisting(dbProgression);

        Optional<ToolProgression> resolvedOpt = toolService.getProgressionFromItem(null);

        assertTrue(resolvedOpt.isPresent());
        ToolProgression resolved = resolvedOpt.get();
        assertEquals(dbOwnerUuid, resolved.getOwnerUuid());
        assertNotEquals(tamperedOwnerUuid, resolved.getOwnerUuid());
    }

    @Test
    @DisplayName("Unknown PDC Tool UUID must be rejected as unauthoritative")
    void testUnknownToolUuidRejected() {
        repository.setExisting(null);

        Optional<ToolProgression> resolvedOpt = toolService.getProgressionFromItem(null);

        assertFalse(resolvedOpt.isPresent());
    }

    private static class FakeMetadataService extends ToolItemMetadataService {
        private UUID toolUuid;
        private UUID ownerUuid;

        public FakeMetadataService() { super(null); }
        public void setToolUuid(UUID toolUuid) { this.toolUuid = toolUuid; }
        public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }

        @Override public boolean isManagedTool(ItemStack itemStack) { return true; }
        @Override public Optional<UUID> getToolUuid(ItemStack itemStack) { return Optional.ofNullable(toolUuid); }
        @Override public Optional<UUID> getOwnerUuid(ItemStack itemStack) { return Optional.ofNullable(ownerUuid); }
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
