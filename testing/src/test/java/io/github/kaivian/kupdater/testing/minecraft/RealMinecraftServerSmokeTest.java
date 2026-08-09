package io.github.kaivian.kupdater.testing.minecraft;

import io.github.kaivian.kupdater.testing.matrix.CompatibilityMatrix;
import io.github.kaivian.kupdater.testing.model.TestResult;
import io.github.kaivian.kupdater.testing.runner.MinecraftServerTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("minecraft")
class RealMinecraftServerSmokeTest {

    @Test
    @DisplayName("Should automatically download Paper server, load KUpdater, verify modules, and shutdown cleanly")
    void testRealMinecraftServerSmoke() {
        CompatibilityMatrix matrix = CompatibilityMatrix.loadDefault();
        String primaryVersion = matrix.getPrimaryVersion();

        MinecraftServerTestRunner runner = new MinecraftServerTestRunner();
        TestResult result = runner.runTest(primaryVersion);

        assertNotNull(result, "TestResult should not be null");
        assertTrue(result.isPassed(), "Real Paper server smoke test failed on version " + primaryVersion + ": " + result.getDiagnosticMessage());
        assertNotNull(result.getLogPath(), "Log path should be preserved");
    }
}
