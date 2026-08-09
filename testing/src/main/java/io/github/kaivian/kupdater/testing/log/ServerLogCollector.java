package io.github.kaivian.kupdater.testing.log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Collects and preserves server logs (latest.log, stdout.log, stderr.log) into build/test-results/minecraft/<version>/.
 */
public class ServerLogCollector {

    private final Path targetLogDir;

    public ServerLogCollector(String minecraftVersion) throws IOException {
        this(Paths.get("build", "test-results", "minecraft", minecraftVersion.replaceAll("[^a-zA-Z0-9._-]", "_")));
    }

    public ServerLogCollector(Path targetLogDir) throws IOException {
        this.targetLogDir = targetLogDir;
        Files.createDirectories(targetLogDir);
    }

    /**
     * Preserves server runtime logs and stdout/stderr line output.
     *
     * @param serverDir runtime server root directory
     * @param stdoutLines captured stdout output lines
     * @param stderrLines captured stderr output lines
     * @return Path to target preserved log directory
     */
    public Path collectLogs(Path serverDir, List<String> stdoutLines, List<String> stderrLines) {
        try {
            Path latestLog = serverDir.resolve("logs").resolve("latest.log");
            if (Files.exists(latestLog)) {
                Files.copy(latestLog, targetLogDir.resolve("server.log"), StandardCopyOption.REPLACE_EXISTING);
            }

            if (stdoutLines != null) {
                Files.write(targetLogDir.resolve("stdout.log"), stdoutLines, StandardCharsets.UTF_8);
            }

            if (stderrLines != null) {
                Files.write(targetLogDir.resolve("stderr.log"), stderrLines, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }

        return targetLogDir;
    }

    public Path getTargetLogDir() {
        return targetLogDir;
    }
}
