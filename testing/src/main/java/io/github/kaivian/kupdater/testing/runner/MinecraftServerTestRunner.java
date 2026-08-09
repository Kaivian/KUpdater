package io.github.kaivian.kupdater.testing.runner;

import io.github.kaivian.kupdater.api.module.ModuleState;
import io.github.kaivian.kupdater.testing.checker.ServerReadinessChecker;
import io.github.kaivian.kupdater.testing.downloader.ServerDownloader;
import io.github.kaivian.kupdater.testing.launcher.IsolatedServerEnvironment;
import io.github.kaivian.kupdater.testing.launcher.ServerProcessManager;
import io.github.kaivian.kupdater.testing.log.ServerLogCollector;
import io.github.kaivian.kupdater.testing.matrix.CompatibilityMatrix;
import io.github.kaivian.kupdater.testing.model.FailureCategory;
import io.github.kaivian.kupdater.testing.model.TestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates automated Paper server downloading, isolated environment setup, plugin installation,
 * process startup, readiness verification, clean shutdown, and log collection.
 */
public class MinecraftServerTestRunner {

    private final CompatibilityMatrix matrix;
    private final ServerDownloader downloader;
    private final Path pluginJarPath;

    public MinecraftServerTestRunner() {
        this(CompatibilityMatrix.loadDefault(), new ServerDownloader(), resolveDefaultPluginJarPath());
    }

    public MinecraftServerTestRunner(CompatibilityMatrix matrix, ServerDownloader downloader, Path pluginJarPath) {
        this.matrix = matrix;
        this.downloader = downloader;
        this.pluginJarPath = pluginJarPath;
    }

    public static Path resolveDefaultPluginJarPath() {
        Path[] candidateDirs = new Path[]{
                Paths.get("..", "bootstrap", "build", "libs"),
                Paths.get("bootstrap", "build", "libs"),
                Paths.get("build", "libs")
        };

        for (Path candidateDir : candidateDirs) {
            if (Files.exists(candidateDir)) {
                File[] files = candidateDir.toFile().listFiles((dir, name) -> name.startsWith("KUpdater-") && name.endsWith(".jar"));
                if (files != null && files.length > 0) {
                    return files[0].toPath().toAbsolutePath();
                }
            }
        }
        return Paths.get("..", "bootstrap", "build", "libs", "KUpdater-0.1.0-SNAPSHOT.jar").toAbsolutePath();
    }

    /**
     * Executes real Minecraft server smoke test for specified version.
     *
     * @param minecraftVersion Minecraft version string (e.g. "1.21.4")
     * @return TestResult object
     */
    public TestResult runTest(String minecraftVersion) {
        long startTime = System.currentTimeMillis();
        Path logDir = null;

        try {
            // 1. Download/acquire server JAR
            Path serverJar;
            try {
                serverJar = downloader.acquireServerJar(minecraftVersion);
            } catch (Exception e) {
                logDir = Paths.get("build", "test-results", "minecraft", minecraftVersion);
                return TestResult.failure(minecraftVersion, FailureCategory.VERSION_COMPATIBILITY_FAILURE,
                        "Failed to download/acquire server JAR: " + e.getMessage(), logDir, System.currentTimeMillis() - startTime);
            }

            // 2. Setup isolated environment
            IsolatedServerEnvironment env = new IsolatedServerEnvironment(minecraftVersion);
            if (pluginJarPath != null && Files.exists(pluginJarPath)) {
                env.installPlugin(pluginJarPath);
            } else {
                logDir = Paths.get("build", "test-results", "minecraft", minecraftVersion);
                return TestResult.failure(minecraftVersion, FailureCategory.PLUGIN_LOAD_FAILURE,
                        "KUpdater plugin JAR not found at " + pluginJarPath + ". Build bootstrap module first (./gradlew :bootstrap:shadowJar)", logDir, System.currentTimeMillis() - startTime);
            }

            // 3. Launch process
            ServerProcessManager processManager = new ServerProcessManager(
                    env.getServerDir(),
                    serverJar,
                    matrix.getStartupTimeoutSeconds(),
                    matrix.getShutdownTimeoutSeconds()
            );

            processManager.startProcess();

            // 4. Await server readiness
            boolean ready = processManager.awaitReadiness();
            List<String> outputLines = processManager.getProcessOutputLines();

            ServerReadinessChecker readinessChecker = new ServerReadinessChecker(outputLines);
            ServerLogCollector logCollector = new ServerLogCollector(minecraftVersion);
            logDir = logCollector.collectLogs(env.getServerDir(), outputLines, processManager.getErrorOutputLines());

            if (!ready) {
                processManager.forceStopProcess();
                return TestResult.failure(minecraftVersion, FailureCategory.MINECRAFT_STARTUP_FAILURE,
                        "Server failed to reach ready state within " + matrix.getStartupTimeoutSeconds() + " seconds",
                        logDir, System.currentTimeMillis() - startTime);
            }

            // 5. Detect plugin load failure exceptions
            FailureCategory failureCategory = readinessChecker.detectFailureCategory();
            if (failureCategory != FailureCategory.NONE) {
                processManager.stopProcessCleanly();
                return TestResult.failure(minecraftVersion, failureCategory,
                        "Detected exception or crash in server logs", logDir, System.currentTimeMillis() - startTime);
            }

            // 6. Programmatically verify plugin startup markers
            boolean pluginLoaded = readinessChecker.hasPluginEnabledMarker();
            List<String> registeredModuleIds = Arrays.asList("tools", "progression", "combat", "farming", "mining", "economy", "exploration");
            Map<String, ModuleState> moduleStates = readinessChecker.parseModuleStates(registeredModuleIds);

            // 7. Stop server cleanly
            boolean cleanShutdown = processManager.stopProcessCleanly();
            long elapsedTime = System.currentTimeMillis() - startTime;

            if (!pluginLoaded) {
                return TestResult.failure(minecraftVersion, FailureCategory.PLUGIN_LOAD_FAILURE,
                        "Plugin enabled marker was not found in server logs", logDir, elapsedTime);
            }

            if (!cleanShutdown) {
                return TestResult.failure(minecraftVersion, FailureCategory.SHUTDOWN_FAILURE,
                        "Server did not exit cleanly within shutdown timeout", logDir, elapsedTime);
            }

            return TestResult.success(minecraftVersion, logDir, elapsedTime, moduleStates);

        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            return TestResult.failure(minecraftVersion, FailureCategory.PLUGIN_LOAD_FAILURE,
                    "Unexpected runner exception: " + e.getMessage(), logDir, elapsedTime);
        }
    }
}
