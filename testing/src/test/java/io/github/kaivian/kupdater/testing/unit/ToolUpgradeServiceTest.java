package io.github.kaivian.kupdater.testing.unit;

import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.api.tools.service.ToolUpgradeService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.validation.ProgressionBalanceValidator;
import io.github.kaivian.kupdater.features.tools.pickaxe.service.PickaxeUpgradeServiceImpl;
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

@Tag("unit")
class ToolUpgradeServiceTest {

    private FakeToolService toolService;
    private FakeToolRepository repository;
    private ToolConfigManager configManager;
    private ToolUpgradeService upgradeService;

    private Player owner;
    private ItemStack managedItem;
    private UUID ownerUuid;
    private UUID toolUuid;

    @BeforeEach
    void setUp() {
        toolService = new FakeToolService();
        repository = new FakeToolRepository();
        configManager = new ToolConfigManager(null);
        configManager.load(null);
        upgradeService = new PickaxeUpgradeServiceImpl(toolService, repository, configManager, new ProgressionBalanceValidator(null), null);

        ownerUuid = UUID.randomUUID();
        toolUuid = UUID.randomUUID();
        owner = ToolOwnershipServiceTest.createMockPlayer(ownerUuid);
        managedItem = Mockito.mock(ItemStack.class);
    }

    @Test
    @DisplayName("Should successfully upgrade Pickaxe from Wooden Level 1 to Level 2")
    void testSuccessfulUpgrade() {
        ToolProgression lvl1 = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, ToolMaterial.WOODEN, 1, ToolState.ACTIVE, 59, 1, Instant.now(), Instant.now());
        toolService.setManagedItem(managedItem);
        toolService.setProgression(lvl1);

        assertTrue(upgradeService.canUpgrade(owner, managedItem));
        Optional<ToolProgression> resultOpt = upgradeService.upgradeTool(owner, managedItem);

        assertTrue(resultOpt.isPresent());
        ToolProgression upgraded = resultOpt.get();
        assertEquals(ToolMaterial.WOODEN, upgraded.getMaterial());
        assertEquals(2, upgraded.getLevel());
        assertEquals(75, upgraded.getCurrentDurability());
    }

    @Test
    @DisplayName("Should transition tier from Wooden Level 3 to Stone Level 1 via tier upgrade")
    void testTierUpgradeFromWoodenToStone() {
        ToolProgression woodenLvl3 = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, ToolMaterial.WOODEN, 3, 150, ToolState.ACTIVE, 95, 1, Instant.now(), Instant.now());
        toolService.setManagedItem(managedItem);
        toolService.setProgression(woodenLvl3);

        Optional<ToolProgression> resultOpt = upgradeService.upgradeTier(owner, managedItem);

        assertTrue(resultOpt.isPresent());
        ToolProgression upgraded = resultOpt.get();
        assertEquals(ToolMaterial.STONE, upgraded.getMaterial());
        assertEquals(1, upgraded.getLevel());
        assertEquals(131, upgraded.getCurrentDurability());
    }

    @Test
    @DisplayName("Should reject upgrade when tool reaches Netherite Level 5 (absolute max tier)")
    void testAbsoluteMaxLevelReached() {
        ToolProgression netheriteMax = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, ToolMaterial.NETHERITE, 5, ToolState.ACTIVE, 3300, 1, Instant.now(), Instant.now());
        toolService.setManagedItem(managedItem);
        toolService.setProgression(netheriteMax);

        assertFalse(upgradeService.canUpgrade(owner, managedItem));
        assertFalse(upgradeService.upgradeTool(owner, managedItem).isPresent());
    }

    @Test
    @DisplayName("Should reject upgrade when tool is DESTROYED or LOST")
    void testInactiveToolUpgradeRejected() {
        ToolProgression destroyed = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, ToolMaterial.WOODEN, 1, ToolState.DESTROYED, 0, 1, Instant.now(), Instant.now());
        toolService.setManagedItem(managedItem);
        toolService.setProgression(destroyed);

        assertFalse(upgradeService.canUpgrade(owner, managedItem));
        assertFalse(upgradeService.upgradeTool(owner, managedItem).isPresent());
    }

    private static class FakeToolService implements ToolService {
        private ItemStack managedItem;
        private ToolProgression progression;

        public void setManagedItem(ItemStack item) { this.managedItem = item; }
        public void setProgression(ToolProgression progression) { this.progression = progression; }

        @Override public Optional<ToolProgression> getProgression(UUID ownerUuid, ToolType toolType) { return Optional.ofNullable(progression); }
        @Override public Optional<ToolProgression> getProgressionByToolUuid(UUID toolUuid) { return Optional.ofNullable(progression); }
        @Override public Optional<ToolProgression> getProgressionFromItem(ItemStack itemStack) { return Optional.ofNullable(progression); }
        @Override public Optional<ToolProgression> registerInitialTool(Player player, ItemStack itemStack) { return Optional.empty(); }
        @Override public void applyMetadataToItem(ItemStack itemStack, ToolProgression progression) {}
        @Override public boolean updateState(UUID toolUuid, ToolState newState) { return true; }
        @Override public boolean isManagedTool(ItemStack itemStack) { return itemStack != null && itemStack.equals(managedItem); }
        @Override public Optional<UUID> getToolUuidFromItem(ItemStack itemStack) { return Optional.ofNullable(progression != null ? progression.getToolUuid() : null); }
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
