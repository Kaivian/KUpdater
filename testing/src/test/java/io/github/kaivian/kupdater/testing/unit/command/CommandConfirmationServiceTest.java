package io.github.kaivian.kupdater.testing.unit.command;

import io.github.kaivian.kupdater.core.command.confirmation.CommandConfirmationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CommandConfirmationServiceTest {

    private CommandConfirmationService confirmationService;

    @BeforeEach
    void setUp() {
        confirmationService = new CommandConfirmationService(500L); // 500ms TTL for fast test
    }

    @Test
    @DisplayName("Should execute immediately when confirmed flag is true")
    void testImmediateExecutionWhenConfirmed() {
        AtomicBoolean executed = new AtomicBoolean(false);
        boolean proceed = confirmationService.handleConfirmation(
                null,
                true,
                "action1",
                "Prompt",
                () -> executed.set(true)
        );

        assertTrue(proceed);
    }

    @Test
    @DisplayName("Should prompt for confirmation when confirmed flag is false")
    void testPromptWhenUnconfirmed() {
        AtomicBoolean executed = new AtomicBoolean(false);
        boolean proceed = confirmationService.handleConfirmation(
                null,
                false,
                "action1",
                "Prompt",
                () -> executed.set(true)
        );

        assertFalse(proceed);
        assertFalse(executed.get());
    }
}
