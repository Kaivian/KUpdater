package io.github.kaivian.kupdater.testing.model;

import io.github.kaivian.kupdater.api.module.ModuleState;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Captures execution results and diagnostic details for a specific Minecraft version test run.
 */
public final class TestResult {

    private final String minecraftVersion;
    private final boolean passed;
    private final FailureCategory failureCategory;
    private final String diagnosticMessage;
    private final Path logPath;
    private final long executionTimeMillis;
    private final Map<String, ModuleState> moduleStates;

    public TestResult(String minecraftVersion, boolean passed, FailureCategory failureCategory,
                      String diagnosticMessage, Path logPath, long executionTimeMillis,
                      Map<String, ModuleState> moduleStates) {
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion cannot be null");
        this.passed = passed;
        this.failureCategory = failureCategory != null ? failureCategory : FailureCategory.NONE;
        this.diagnosticMessage = diagnosticMessage != null ? diagnosticMessage : "";
        this.logPath = logPath;
        this.executionTimeMillis = executionTimeMillis;
        this.moduleStates = moduleStates != null ? moduleStates : Collections.emptyMap();
    }

    public static TestResult success(String minecraftVersion, Path logPath, long executionTimeMillis, Map<String, ModuleState> moduleStates) {
        return new TestResult(minecraftVersion, true, FailureCategory.NONE, "Passed successfully", logPath, executionTimeMillis, moduleStates);
    }

    public static TestResult failure(String minecraftVersion, FailureCategory category, String diagnosticMessage, Path logPath, long executionTimeMillis) {
        return new TestResult(minecraftVersion, false, category, diagnosticMessage, logPath, executionTimeMillis, Collections.emptyMap());
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public boolean isPassed() {
        return passed;
    }

    public FailureCategory getFailureCategory() {
        return failureCategory;
    }

    public String getDiagnosticMessage() {
        return diagnosticMessage;
    }

    public Path getLogPath() {
        return logPath;
    }

    public long getExecutionTimeMillis() {
        return executionTimeMillis;
    }

    public Map<String, ModuleState> getModuleStates() {
        return moduleStates;
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "minecraftVersion='" + minecraftVersion + '\'' +
                ", passed=" + passed +
                ", failureCategory=" + failureCategory +
                ", diagnosticMessage='" + diagnosticMessage + '\'' +
                ", executionTimeMillis=" + executionTimeMillis +
                '}';
    }
}
