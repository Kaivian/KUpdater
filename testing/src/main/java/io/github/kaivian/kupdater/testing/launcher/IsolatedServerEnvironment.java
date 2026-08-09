package io.github.kaivian.kupdater.testing.launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Random;

/**
 * Prepares isolated runtime directories, EULA, server.properties, and plugin JAR installation.
 */
public class IsolatedServerEnvironment {

    private final String minecraftVersion;
    private final Path serverDir;
    private final int serverPort;

    public IsolatedServerEnvironment(String minecraftVersion) throws IOException {
        this(minecraftVersion, Paths.get("build", "minecraft-test", minecraftVersion.replaceAll("[^a-zA-Z0-9._-]", "_")));
    }

    public IsolatedServerEnvironment(String minecraftVersion, Path serverDir) throws IOException {
        this.minecraftVersion = minecraftVersion;
        this.serverDir = serverDir;
        this.serverPort = 25000 + new Random().nextInt(5000);
        setupEnvironment();
    }

    private void setupEnvironment() throws IOException {
        if (Files.exists(serverDir)) {
            // Clean runtime state from previous test runs while preserving dir structure
            deleteDirectoryContents(serverDir);
        } else {
            Files.createDirectories(serverDir);
        }

        Files.createDirectories(serverDir.resolve("plugins"));
        Files.createDirectories(serverDir.resolve("logs"));

        // Write eula.txt automatically for test environment
        Files.writeString(serverDir.resolve("eula.txt"), "eula=true\n", StandardCharsets.UTF_8);

        // Write server.properties with fast and safe test settings
        String serverProps = "server-port=" + serverPort + "\n" +
                "online-mode=false\n" +
                "enable-rcon=false\n" +
                "spawn-protection=0\n" +
                "max-players=5\n" +
                "view-distance=4\n" +
                "allow-nether=false\n" +
                "generate-structures=false\n" +
                "level-type=minecraft\\:flat\n" +
                "motd=KUpdater Test Server " + minecraftVersion + "\n";
        Files.writeString(serverDir.resolve("server.properties"), serverProps, StandardCharsets.UTF_8);
    }

    /**
     * Installs KUpdater plugin shadow JAR into plugins/ folder.
     *
     * @param pluginJarPath Path to KUpdater shadow JAR
     * @return Path to installed plugin JAR
     * @throws IOException if copy fails
     */
    public Path installPlugin(Path pluginJarPath) throws IOException {
        if (pluginJarPath == null || !Files.exists(pluginJarPath)) {
            throw freshPluginNotFoundError();
        }

        Path targetPluginJar = serverDir.resolve("plugins").resolve("KUpdater.jar");
        Files.copy(pluginJarPath, targetPluginJar, StandardCopyOption.REPLACE_EXISTING);
        return targetPluginJar;
    }

    private IOException freshPluginNotFoundError() {
        return new IOException("KUpdater plugin JAR not found. Build bootstrap module first (./gradlew :bootstrap:shadowJar)");
    }

    private void deleteDirectoryContents(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a))
                .filter(p -> !p.equals(directory))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException ignored) {
                    }
                });
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public Path getServerDir() {
        return serverDir;
    }

    public int getServerPort() {
        return serverPort;
    }
}
