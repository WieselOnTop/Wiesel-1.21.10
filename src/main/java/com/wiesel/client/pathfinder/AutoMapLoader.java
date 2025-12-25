package com.wiesel.client.pathfinder;

import com.wiesel.client.WieselClient;
import com.wiesel.client.util.TablistReader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoMapLoader {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WieselMapLoader");
        t.setDaemon(true);
        return t;
    });

    private static String lastLoadedMap = null;
    private static long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 3000; // Check every 3 seconds

    public static void tick() {
        long currentTime = System.currentTimeMillis();

        // Don't check too frequently
        if (currentTime - lastCheckTime < CHECK_INTERVAL) {
            return;
        }

        lastCheckTime = currentTime;

        if (!TablistReader.isInWorld()) {
            return;
        }

        String currentArea = TablistReader.getCurrentArea();
        if (currentArea == null) {
            return;
        }

        // Check if we need to load a new map
        if (currentArea.equals(lastLoadedMap)) {
            return; // Already loaded
        }

        // Map the area name to our map files
        String mapName = mapAreaToMapName(currentArea);
        if (mapName == null) {
            return; // No matching map
        }

        WieselClient.LOGGER.info("Detected area change: {} -> Loading map: {}", currentArea, mapName);

        // Load map in background thread
        loadMapAsync(mapName, currentArea);
    }

    private static String mapAreaToMapName(String area) {
        // Map detected area names to our map files
        switch (area.toLowerCase()) {
            case "hub":
                return "hub";
            case "mines":
            case "mine":
            case "dwarven":
            case "crystal":
                return "mines";
            case "galatea":
            case "garden":
                return "galatea";
            default:
                return null;
        }
    }

    private static void loadMapAsync(String mapName, String areaName) {
        EXECUTOR.submit(() -> {
            try {
                WieselClient.LOGGER.info("Loading map '{}' in background...", mapName);
                boolean success = PathfinderManager.loadMap(mapName);

                if (success) {
                    lastLoadedMap = areaName;
                    WieselClient.LOGGER.info("Successfully loaded map: {}", mapName);
                } else {
                    WieselClient.LOGGER.error("Failed to load map: {}", mapName);
                }
            } catch (Exception e) {
                WieselClient.LOGGER.error("Error loading map asynchronously", e);
            }
        });
    }

    public static void reset() {
        lastLoadedMap = null;
    }

    public static String getLastLoadedMap() {
        return lastLoadedMap;
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
    }
}
