package io.github.kaivian.kupdater.testing.unit.command;

import io.github.kaivian.kupdater.api.command.KCommandContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CommandContextTest {

    @Test
    @DisplayName("Should correctly retrieve typed arguments from context")
    void testGetArgument() {
        Map<String, Object> args = new HashMap<>();
        args.put("amount", 100);
        args.put("subcommand", "xp");

        KCommandContext context = new KCommandContext(null, "kupdater", args);

        Optional<Integer> amountOpt = context.getArgument("amount", Integer.class);
        assertTrue(amountOpt.isPresent());
        assertEquals(100, amountOpt.get());

        Optional<String> subOpt = context.getArgument("subcommand", String.class);
        assertTrue(subOpt.isPresent());
        assertEquals("xp", subOpt.get());

        Optional<Boolean> nonExistent = context.getArgument("missing", Boolean.class);
        assertFalse(nonExistent.isPresent());
    }
}
