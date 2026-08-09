package io.github.kaivian.kupdater.testing.launcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages child Java processes for Minecraft server testing with asynchronous output capture,
 * timeout enforcement, graceful shutdown, and JVM cleanup hooks.
 */
public class ServerProcessManager {

    private final Path serverDir;
    private final Path serverJar;
    private final int startupTimeoutSeconds;
    private final int shutdownTimeoutSeconds;

    private Process process;
    private BufferedWriter stdinWriter;
    private final List<String> processOutputLines = new CopyOnWriteArrayList<>();
    private final List<String> errorOutputLines = new CopyOnWriteArrayList<>();
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    private Thread shutdownHook;

    public ServerProcessManager(Path serverDir, Path serverJar, int startupTimeoutSeconds, int shutdownTimeoutSeconds) {
        this.serverDir = serverDir;
        this.serverJar = serverJar;
        this.startupTimeoutSeconds = startupTimeoutSeconds;
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
    }

    /**
     * Launches the Minecraft server process.
     *
     * @throws IOException if process launching fails
     */
    public void startProcess() throws IOException {
        String javaBinary = System.getProperty("java.home") + "/bin/java";

        ProcessBuilder pb = new ProcessBuilder(
                javaBinary,
                "-Xms1G",
                "-Xmx2G",
                "-Dcom.mojang.eula.agree=true",
                "-jar",
                serverJar.toAbsolutePath().toString(),
                "nogui"
        );
        pb.directory(serverDir.toFile());

        this.process = pb.start();
        this.stdinWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        // Register JVM shutdown hook to prevent orphaned child processes
        this.shutdownHook = new Thread(() -> forceStopProcess(), "ServerProcessCleanupHook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // Asynchronous stdout reader
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processOutputLines.add(line);
                    if (!ready.get() && (line.contains("Done (") || line.contains("[Server thread/INFO]: Done"))) {
                        ready.set(true);
                        readyLatch.countDown();
                    }
                }
            } catch (IOException ignored) {
            }
        }, "ServerStdoutReader").start();

        // Asynchronous stderr reader
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutputLines.add(line);
                }
            } catch (IOException ignored) {
            }
        }, "ServerStderrReader").start();
    }

    /**
     * Blocks until server readiness marker is detected or startup timeout is reached.
     *
     * @return true if server became ready, false on timeout
     */
    public boolean awaitReadiness() throws InterruptedException {
        return readyLatch.await(startupTimeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Sends command to server stdin.
     *
     * @param command command string (e.g. "stop")
     */
    public void sendCommand(String command) {
        if (stdinWriter != null && process != null && process.isAlive()) {
            try {
                stdinWriter.write(command + "\n");
                stdinWriter.flush();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Gracefully stops the server process by sending /stop command.
     *
     * @return true if exited cleanly, false if forced destruction was required
     */
    public boolean stopProcessCleanly() {
        if (process == null || !process.isAlive()) {
            removeShutdownHook();
            return true;
        }

        try {
            sendCommand("stop");
            boolean exited = process.waitFor(shutdownTimeoutSeconds, TimeUnit.SECONDS);
            if (exited) {
                removeShutdownHook();
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        forceStopProcess();
        removeShutdownHook();
        return false;
    }

    /**
     * Forcefully terminates child process if running.
     */
    public void forceStopProcess() {
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void removeShutdownHook() {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    public int getExitCode() {
        return process != null && !process.isAlive() ? process.exitValue() : -1;
    }

    public List<String> getProcessOutputLines() {
        return Collections.unmodifiableList(new ArrayList<>(processOutputLines));
    }

    public List<String> getErrorOutputLines() {
        return Collections.unmodifiableList(new ArrayList<>(errorOutputLines));
    }
}
