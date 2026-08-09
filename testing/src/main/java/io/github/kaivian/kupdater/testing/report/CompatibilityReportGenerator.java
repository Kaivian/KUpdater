package io.github.kaivian.kupdater.testing.report;

import io.github.kaivian.kupdater.api.module.ModuleState;
import io.github.kaivian.kupdater.testing.model.TestResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Generates human-readable Markdown tables and machine-readable JSON reports for version matrix runs.
 */
public class CompatibilityReportGenerator {

    private final Path reportDir;

    public CompatibilityReportGenerator() throws IOException {
        this(Paths.get("build", "reports", "compatibility"));
    }

    public CompatibilityReportGenerator(Path reportDir) throws IOException {
        this.reportDir = reportDir;
        Files.createDirectories(reportDir);
    }

    /**
     * Generates compatibility reports from a list of test results.
     *
     * @param results list of TestResult objects
     * @throws IOException if writing files fails
     */
    public void generateReports(List<TestResult> results) throws IOException {
        generateMarkdownReport(results);
        generateJsonReport(results);
    }

    public String generateMarkdownReport(List<TestResult> results) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# KUpdater Compatibility Report\n\n");
        sb.append("| Minecraft Version | Status | Failure Category | Execution Time | Module States |\n");
        sb.append("| ----------------- | ------ | ---------------- | -------------- | ------------- |\n");

        for (TestResult r : results) {
            String statusStr = r.isPassed() ? "**PASS**" : "**FAIL**";
            String categoryStr = r.getFailureCategory().name();
            String timeStr = r.getExecutionTimeMillis() + " ms";

            StringBuilder modulesSummary = new StringBuilder();
            if (!r.getModuleStates().isEmpty()) {
                for (Map.Entry<String, ModuleState> entry : r.getModuleStates().entrySet()) {
                    if (modulesSummary.length() > 0) modulesSummary.append(", ");
                    modulesSummary.append(entry.getKey()).append("=").append(entry.getValue());
                }
            } else {
                modulesSummary.append("-");
            }

            sb.append("| ")
                    .append(r.getMinecraftVersion()).append(" | ")
                    .append(statusStr).append(" | ")
                    .append(categoryStr).append(" | ")
                    .append(timeStr).append(" | ")
                    .append(modulesSummary).append(" |\n");
        }

        String markdown = sb.toString();
        Files.writeString(reportDir.resolve("compatibility-report.md"), markdown, StandardCharsets.UTF_8);
        return markdown;
    }

    public String generateJsonReport(List<TestResult> results) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"project\": \"KUpdater\",\n");
        sb.append("  \"results\": [\n");

        for (int i = 0; i < results.size(); i++) {
            TestResult r = results.get(i);
            sb.append("    {\n");
            sb.append("      \"minecraft\": \"").append(escapeJson(r.getMinecraftVersion())).append("\",\n");
            sb.append("      \"passed\": ").append(r.isPassed()).append(",\n");
            sb.append("      \"failureCategory\": \"").append(r.getFailureCategory().name()).append("\",\n");
            sb.append("      \"diagnosticMessage\": \"").append(escapeJson(r.getDiagnosticMessage())).append("\",\n");
            sb.append("      \"executionTimeMillis\": ").append(r.getExecutionTimeMillis()).append(",\n");
            sb.append("      \"moduleStates\": {\n");

            int j = 0;
            for (Map.Entry<String, ModuleState> entry : r.getModuleStates().entrySet()) {
                sb.append("        \"").append(escapeJson(entry.getKey())).append("\": \"")
                        .append(entry.getValue().name()).append("\"");
                if (++j < r.getModuleStates().size()) sb.append(",");
                sb.append("\n");
            }
            sb.append("      }\n");
            sb.append("    }");
            if (i < results.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");

        String json = sb.toString();
        Files.writeString(reportDir.resolve("compatibility-report.json"), json, StandardCharsets.UTF_8);
        return json;
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    public Path getReportDir() {
        return reportDir;
    }
}
