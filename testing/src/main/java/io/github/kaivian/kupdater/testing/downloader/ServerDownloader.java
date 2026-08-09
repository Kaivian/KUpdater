package io.github.kaivian.kupdater.testing.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads and caches Paper server binaries using the official PaperMC API v3.
 */
public class ServerDownloader {

    private static final String PAPER_API_V3_BASE = "https://fill.papermc.io/v3/projects/paper";
    private static final String USER_AGENT = "KUpdaterTesting/1.0 (https://github.com/kaivian/KUpdater)";

    private final Path cacheDir;
    private final HttpClient httpClient;

    public ServerDownloader() {
        this(resolveDefaultCacheDir());
    }

    public ServerDownloader(Path cacheDir) {
        this.cacheDir = cacheDir;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public static Path resolveDefaultCacheDir() {
        String userHome = System.getProperty("user.home");
        Path defaultDir = Paths.get(userHome, ".gradle", "caches", "minecraft-servers");
        try {
            Files.createDirectories(defaultDir);
        } catch (IOException ignored) {
            defaultDir = Paths.get("build", "minecraft-servers");
        }
        return defaultDir;
    }

    /**
     * Resolves and downloads Paper server JAR for specified Minecraft version.
     * Uses local cache if already present and non-empty.
     *
     * @param minecraftVersion Minecraft version string (e.g., "1.21.4")
     * @return Path to local server JAR artifact
     * @throws IOException if downloading or resolving fails
     */
    public Path acquireServerJar(String minecraftVersion) throws IOException, InterruptedException {
        String cleanVersion = minecraftVersion != null ? minecraftVersion.trim() : "";
        if (cleanVersion.isEmpty()) {
            throw new IllegalArgumentException("Minecraft version cannot be null or empty");
        }

        Files.createDirectories(cacheDir);
        Path cachedJar = cacheDir.resolve("paper-" + cleanVersion + ".jar");

        if (Files.exists(cachedJar) && Files.size(cachedJar) > 0) {
            return cachedJar;
        }

        // Fetch builds array from PaperMC API v3
        String buildsUrl = PAPER_API_V3_BASE + "/versions/" + cleanVersion + "/builds";
        HttpRequest buildRequest = HttpRequest.newBuilder()
                .uri(URI.create(buildsUrl))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> buildResponse = httpClient.send(buildRequest, HttpResponse.BodyHandlers.ofString());
        if (buildResponse.statusCode() != 200) {
            throw new IOException("Unsupported or unresolvable Paper server version: '" + cleanVersion
                    + "' (PaperMC API returned status " + buildResponse.statusCode() + ")");
        }

        String downloadUrl = extractLatestDownloadUrl(buildResponse.body());
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            throw new IOException("Failed to parse Paper download URL for version: " + cleanVersion);
        }

        HttpRequest jarRequest = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofMinutes(3))
                .GET()
                .build();

        Path tempDownload = Files.createTempFile(cacheDir, "download-paper-", ".tmp");
        HttpResponse<InputStream> jarResponse = httpClient.send(jarRequest, HttpResponse.BodyHandlers.ofInputStream());

        if (jarResponse.statusCode() != 200) {
            Files.deleteIfExists(tempDownload);
            throw new IOException("Failed to download Paper server JAR from: " + downloadUrl + " (HTTP " + jarResponse.statusCode() + ")");
        }

        try (InputStream in = jarResponse.body()) {
            Files.copy(in, tempDownload, StandardCopyOption.REPLACE_EXISTING);
        }

        Files.move(tempDownload, cachedJar, StandardCopyOption.REPLACE_EXISTING);
        return cachedJar;
    }

    private String extractLatestDownloadUrl(String json) {
        // Match the first "url": "https://fill-data.papermc.io/v1/objects/.../paper-..."
        Pattern pattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public Path getCacheDir() {
        return cacheDir;
    }
}
