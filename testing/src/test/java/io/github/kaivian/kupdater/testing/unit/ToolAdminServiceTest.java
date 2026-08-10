package io.github.kaivian.kupdater.testing.unit;

import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.core.command.confirmation.CommandConfirmationService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.service.ToolAdminService;
import io.github.kaivian.kupdater.features.tools.common.service.ToolAdminServiceImpl;
import io.github.kaivian.kupdater.features.tools.common.service.ToolItemSynchronizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class ToolAdminServiceTest {

    private ToolService toolService;
    private ToolRepository repository;
    private ToolConfigManager configManager;
    private ToolItemSynchronizer itemSynchronizer;
    private CommandConfirmationService confirmationService;
    private ToolAdminService adminService;
    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        toolService = Mockito.mock(ToolService.class);
        repository = Mockito.mock(ToolRepository.class);
        configManager = new ToolConfigManager(null);
        configManager.load(null);
        itemSynchronizer = Mockito.mock(ToolItemSynchronizer.class);
        confirmationService = new CommandConfirmationService();
        adminService = new ToolAdminServiceImpl(toolService, repository, configManager, itemSynchronizer, confirmationService, null);

        playerUuid = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should reject xp set when amount exceeds current level requirement")
    void testXpSetExceedsRequirementRejected() {
        ToolProgression prog = new ToolProgression(
                UUID.randomUUID(), playerUuid, ToolType.PICKAXE, ToolMaterial.WOODEN, 1, 0, 0, ToolState.ACTIVE, 59, 1, Instant.now(), Instant.now()
        );
        Mockito.when(repository.findByOwnerAndType(playerUuid, ToolType.PICKAXE)).thenReturn(Optional.of(prog));

        ToolAdminService.Result result = adminService.setXp(playerUuid, 1000, null);
        assertFalse(result.isSuccess());
        assertEquals(ToolAdminService.Status.EXCEEDS_REQUIREMENT, result.getStatus());
    }

    @Test
    @DisplayName("Should trigger multi level-up when adding large XP amount")
    void testXpAddMultiLevelUp() {
        ToolProgression prog = new ToolProgression(
                UUID.randomUUID(), playerUuid, ToolType.PICKAXE, ToolMaterial.WOODEN, 1, 0, 0, ToolState.ACTIVE, 59, 1, Instant.now(), Instant.now()
        );
        Mockito.when(repository.findByOwnerAndType(playerUuid, ToolType.PICKAXE)).thenReturn(Optional.of(prog));
        Mockito.when(repository.save(any(ToolProgression.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ToolAdminService.Result result = adminService.addXp(playerUuid, 400, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getProgression().isPresent());
        assertTrue(result.getProgression().get().getLevel() > 1);
    }
}
