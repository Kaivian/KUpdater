package io.github.kaivian.kupdater.testing.compatibility;

import io.github.kaivian.kupdater.testing.matrix.CompatibilityMatrix;
import io.github.kaivian.kupdater.testing.model.TestResult;
import io.github.kaivian.kupdater.testing.report.CompatibilityReportGenerator;
import io.github.kaivian.kupdater.testing.runner.MinecraftServerTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("compatibility")
class VersionMatrixCompatibilityTest {

    @Test
    @DisplayName("Should run compatibility matrix across representative Minecraft versions and generate JSON/Markdown reports")
    void testVersionMatrixCompatibility() throws IOException {
        CompatibilityMatrix matrix = CompatibilityMatrix.loadDefault();
        String level = System.getProperty("matrixLevel", "smoke");
        List<String> targetVersions = matrix.resolveLevel(level);

        MinecraftServerTestRunner runner = new MinecraftServerTestRunner();
        List<TestResult> results = new ArrayList<>();

        for (String version : targetVersions) {
            TestResult result = runner.runTest(version);
            results.add(result);
        }

        CompatibilityReportGenerator reportGenerator = new CompatibilityReportGenerator();
        reportGenerator.generateReports(results);

        Path jsonReport = reportGenerator.getReportDir().resolve("compatibility-report.json");
        Path mdReport = reportGenerator.getReportDir().resolve("compatibility-report.md");

        assertTrue(Files.exists(jsonReport), "JSON compatibility report should exist");
        assertTrue(Files.exists(mdReport), "Markdown compatibility report should exist");

        boolean anyFailed = results.stream().anyMatch(r -> !r.isPassed());
        assertFalse(anyFailed, "At least one version compatibility test failed in matrix run");
    }
}
