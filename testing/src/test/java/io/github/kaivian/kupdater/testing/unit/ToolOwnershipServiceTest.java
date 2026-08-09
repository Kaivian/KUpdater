package io.github.kaivian.kupdater.testing.unit;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.service.ToolOwnershipService;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.service.ToolOwnershipServiceImpl;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ToolOwnershipServiceTest {

    private FakeToolService toolService;
    private ToolConfigManager configManager;
    private ToolOwnershipService ownershipService;

    private Player owner;
    private Player nonOwner;
    private ItemStack managedItem;
    private ItemStack unmanagedItem;

    private UUID ownerUuid;
    private UUID nonOwnerUuid;
    private UUID toolUuid;

    @BeforeEach
    void setUp() {
        toolService = new FakeToolService();
        configManager = new ToolConfigManager(null);
        configManager.load(null);
        ownershipService = new ToolOwnershipServiceImpl(toolService, configManager);

        ownerUuid = UUID.randomUUID();
        nonOwnerUuid = UUID.randomUUID();
        toolUuid = UUID.randomUUID();

        owner = createMockPlayer(ownerUuid);
        nonOwner = createMockPlayer(nonOwnerUuid);

        managedItem = Mockito.mock(ItemStack.class);
        unmanagedItem = Mockito.mock(ItemStack.class);
    }

    @Test
    @DisplayName("Owner should be authorized to use their active Pickaxe")
    void testOwnerCanUseActiveTool() {
        ToolProgression activeProg = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, 1, ToolState.ACTIVE, 59, 1, Instant.now(), Instant.now());
        toolService.setManagedItem(managedItem);
        toolService.setProgression(activeProg);

        assertTrue(ownershipService.isOwner(owner, managedItem));
        assertTrue(ownershipService.canUse(owner, managedItem));
    }

    @Test
    @DisplayName("Non-owner should be denied usage of managed Pickaxe")
    void testNonOwnerDeniedUsage() {
        ToolProgression activeProg = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, 1, ToolState.ACTIVE, 59, 1, Instant.now(), Instant.now());
        toolService.setManagedItem(managedItem);
        toolService.setProgression(activeProg);

        assertFalse(ownershipService.isOwner(nonOwner, managedItem));
        assertFalse(ownershipService.canUse(nonOwner, managedItem));
        assertEquals("&cThis Pickaxe does not belong to you.", ownershipService.getOwnershipDenyMessage());
    }

    @Test
    @DisplayName("Owner should be denied usage when tool is in DESTROYED state")
    void testOwnerDeniedUsageWhenDestroyed() {
        ToolProgression destroyedProg = new ToolProgression(toolUuid, ownerUuid, ToolType.PICKAXE, 1, ToolState.DESTROYED, 0, 1, Instant.now(), Instant.now());
        toolService.setManagedItem(managedItem);
        toolService.setProgression(destroyedProg);

        assertTrue(ownershipService.isOwner(owner, managedItem));
        assertFalse(ownershipService.canUse(owner, managedItem));
    }

    @Test
    @DisplayName("Anyone should be able to use an unmanaged item")
    void testUnmanagedItemUsage() {
        toolService.setManagedItem(null);

        assertTrue(ownershipService.isOwner(owner, unmanagedItem));
        assertTrue(ownershipService.canUse(owner, unmanagedItem));
        assertTrue(ownershipService.canUse(nonOwner, unmanagedItem));
    }

    static Player createMockPlayer(UUID uuid) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) {
                        return uuid;
                    }
                    if ("getName".equals(method.getName())) {
                        return "TestPlayer";
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (method.getReturnType().equals(int.class)) {
                        return 0;
                    }
                    return null;
                }
        );
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
}
