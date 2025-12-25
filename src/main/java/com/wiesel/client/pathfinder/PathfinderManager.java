package com.wiesel.client.pathfinder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wiesel.client.WieselClient;
import com.wiesel.client.config.ConfigManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PathfinderManager {
    private static final String API_URL = "http://localhost:3000";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static Process pathfinderProcess = null;
    private static Timer keepaliveTimer = null;
    private static String currentMap = null;
    private static PathfindResponse lastPath = null;

    public static void initialize() {
        // Extract maps in background thread to avoid blocking
        new Thread(() -> {
            try {
                extractMapsIfNeeded();
            } catch (Exception e) {
                WieselClient.LOGGER.error("Error extracting maps", e);
            }
        }, "WieselMapExtractor").start();

        // Start pathfinder process
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Wait a moment for maps to start extracting
                startPathfinder();
            } catch (Exception e) {
                WieselClient.LOGGER.error("Error starting pathfinder", e);
            }
        }, "WieselPathfinderStarter").start();

        // Start keepalive timer
        startKeepaliveTimer();
    }

    private static void extractMapsIfNeeded() {
        File mapsDir = new File(ConfigManager.getMinecraftDir(), "maps");
        if (!mapsDir.exists()) {
            mapsDir.mkdirs();
            WieselClient.LOGGER.info("Created maps directory: {}", mapsDir.getAbsolutePath());
        }

        WieselClient.LOGGER.info("Starting map extraction from Downloads folder...");

        // Extract bundled maps
        extractMapFromDownloads("hub.zip", mapsDir);
        extractMapFromDownloads("mines.zip", mapsDir);
        extractMapFromDownloads("galatea.zip", mapsDir);

        WieselClient.LOGGER.info("Map extraction completed");
    }

    private static void extractMapFromDownloads(String zipName, File targetDir) {
        File downloadsDir = new File(System.getProperty("user.home"), "Downloads");
        File zipFile = new File(downloadsDir, zipName);

        if (!zipFile.exists()) {
            WieselClient.LOGGER.warn("Map file not found: {}", zipFile.getAbsolutePath());
            return;
        }

        String mapName = zipName.replace(".zip", "");
        File mapDir = new File(targetDir, mapName);

        if (mapDir.exists() && mapDir.listFiles() != null && mapDir.listFiles().length > 0) {
            WieselClient.LOGGER.info("Map '{}' already extracted, skipping", mapName);
            return;
        }

        try {
            mapDir.mkdirs();
            unzip(zipFile, mapDir);
            WieselClient.LOGGER.info("Extracted map '{}' to {}", mapName, mapDir.getAbsolutePath());
        } catch (Exception e) {
            WieselClient.LOGGER.error("Failed to extract map: {}", zipName, e);
        }
    }

    private static void unzip(File zipFile, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    public static void startPathfinder() {
        if (pathfinderProcess != null && pathfinderProcess.isAlive()) {
            WieselClient.LOGGER.info("Pathfinder already running");
            return;
        }

        File pathfinderExe = new File(System.getProperty("user.home"), "Downloads/Pathfinding.exe");
        if (!pathfinderExe.exists()) {
            WieselClient.LOGGER.error("Pathfinder executable not found at: {}", pathfinderExe.getAbsolutePath());
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(pathfinderExe.getAbsolutePath());
            pb.directory(new File(ConfigManager.getMinecraftDir(), "maps").getParentFile());
            pathfinderProcess = pb.start();
            WieselClient.LOGGER.info("Started pathfinder process");

            // Wait a bit for the server to start
            Thread.sleep(2000);
        } catch (Exception e) {
            WieselClient.LOGGER.error("Failed to start pathfinder", e);
        }
    }

    private static void startKeepaliveTimer() {
        if (keepaliveTimer != null) {
            keepaliveTimer.cancel();
        }

        keepaliveTimer = new Timer("PathfinderKeepalive", true);
        keepaliveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendKeepalive();
            }
        }, 5000, 60000); // Start after 5s, repeat every 60s
    }

    private static void sendKeepalive() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/keepalive"))
                .GET()
                .build();

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        WieselClient.LOGGER.debug("Keepalive sent successfully");
                    }
                });
        } catch (Exception e) {
            WieselClient.LOGGER.error("Failed to send keepalive", e);
        }
    }

    public static boolean loadMap(String mapName) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/api/loadmap?map=" + mapName))
                .GET()
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                currentMap = mapName;
                WieselClient.LOGGER.info("Loaded map: {}", mapName);
                return true;
            } else {
                WieselClient.LOGGER.error("Failed to load map '{}': {}", mapName, response.body());
                return false;
            }
        } catch (Exception e) {
            WieselClient.LOGGER.error("Failed to load map: {}", mapName, e);
            return false;
        }
    }

    public static PathfindResponse pathfind(double x1, double y1, double z1, double x2, double y2, double z2) {
        return pathfind(x1, y1, z1, x2, y2, z2, false, false, true, false, false);
    }

    public static PathfindResponse pathfind(double x1, double y1, double z1, double x2, double y2, double z2,
                                           boolean useWarpPoints, boolean useEtherwarp, boolean useKeynodes,
                                           boolean useSpline, boolean isPerfectPath) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("start", String.format("%.0f,%.0f,%.0f", x1, y1, z1));
            requestBody.addProperty("end", String.format("%.0f,%.0f,%.0f", x2, y2, z2));
            requestBody.addProperty("use_warp_points", useWarpPoints);
            requestBody.addProperty("use_etherwarp", useEtherwarp);
            requestBody.addProperty("use_keynodes", useKeynodes);
            requestBody.addProperty("use_spline", useSpline);
            requestBody.addProperty("is_perfect_path", isPerfectPath);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/api/pathfind"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = GSON.fromJson(response.body(), JsonObject.class);

                List<PathNode> path = parseNodes(json.getAsJsonArray("path"));
                List<PathNode> keynodes = parseNodes(json.getAsJsonArray("keynodes"));

                lastPath = new PathfindResponse(path, keynodes);
                WieselClient.LOGGER.info("Pathfinding successful: {} nodes, {} keynodes", path.size(), keynodes.size());
                return lastPath;
            } else {
                WieselClient.LOGGER.error("Pathfinding failed: {}", response.body());
                return null;
            }
        } catch (Exception e) {
            WieselClient.LOGGER.error("Failed to pathfind", e);
            return null;
        }
    }

    private static List<PathNode> parseNodes(JsonArray array) {
        List<PathNode> nodes = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            nodes.add(new PathNode(
                obj.get("x").getAsInt(),
                obj.get("y").getAsInt(),
                obj.get("z").getAsInt(),
                obj.has("top_bound") ? obj.get("top_bound").getAsFloat() : 0,
                obj.has("path_weight") ? obj.get("path_weight").getAsFloat() : 0,
                obj.has("is_liquid") && obj.get("is_liquid").getAsBoolean()
            ));
        }
        return nodes;
    }

    public static PathfindResponse getLastPath() {
        return lastPath;
    }

    public static void clearPath() {
        lastPath = null;
    }

    public static String getCurrentMap() {
        return currentMap;
    }

    public static void shutdown() {
        if (keepaliveTimer != null) {
            keepaliveTimer.cancel();
            keepaliveTimer = null;
        }

        if (pathfinderProcess != null) {
            pathfinderProcess.destroy();
            pathfinderProcess = null;
            WieselClient.LOGGER.info("Stopped pathfinder process");
        }
    }
}
