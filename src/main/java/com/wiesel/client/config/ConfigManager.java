package com.wiesel.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wiesel.client.WieselClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File(FabricLoader.getInstance().getGameDir().toFile(), "config/wiesel");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "config.json");

    private static WieselConfig config;

    public static void load() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, WieselConfig.class);
                WieselClient.LOGGER.info("Loaded config from {}", CONFIG_FILE.getAbsolutePath());
            } catch (Exception e) {
                WieselClient.LOGGER.error("Failed to load config, using defaults", e);
                config = new WieselConfig();
            }
        } else {
            config = new WieselConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
            WieselClient.LOGGER.info("Saved config to {}", CONFIG_FILE.getAbsolutePath());
        } catch (Exception e) {
            WieselClient.LOGGER.error("Failed to save config", e);
        }
    }

    public static WieselConfig getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static File getConfigDir() {
        return CONFIG_DIR;
    }

    public static File getMinecraftDir() {
        return FabricLoader.getInstance().getGameDir().toFile();
    }

    /**
     * Get a float value from config using dot notation (e.g. "rotation.yawSpeed")
     */
    public static float getFloat(String path, float defaultValue) {
        try {
            String[] parts = path.split("\\.");
            if (parts.length != 2) return defaultValue;

            WieselConfig cfg = getConfig();
            switch (parts[0]) {
                case "rotation":
                    switch (parts[1]) {
                        case "yawSpeed": return cfg.rotation.yawSpeed;
                        case "pitchSpeed": return cfg.rotation.pitchSpeed;
                        case "lookahead": return cfg.rotation.lookahead;
                        case "lookaheadMinDist": return cfg.rotation.lookaheadMinDist;
                        case "lookaheadMaxDist": return cfg.rotation.lookaheadMaxDist;
                        case "cornerBoost": return cfg.rotation.cornerBoost;
                    }
                    break;
                case "etherwarp":
                    switch (parts[1]) {
                        case "rotationSpeed": return cfg.etherwarp.rotationSpeed;
                        case "overshootAmount": return cfg.etherwarp.overshootAmount;
                        case "speedVariation": return cfg.etherwarp.speedVariation;
                    }
                    break;
            }
        } catch (Exception e) {
            WieselClient.LOGGER.error("Error getting config value: {}", path, e);
        }
        return defaultValue;
    }

    /**
     * Get a boolean value from config using dot notation (e.g. "rotation.enableLOS")
     */
    public static boolean getBoolean(String path, boolean defaultValue) {
        try {
            String[] parts = path.split("\\.");
            if (parts.length != 2) return defaultValue;

            WieselConfig cfg = getConfig();
            switch (parts[0]) {
                case "rotation":
                    if ("enableLOS".equals(parts[1])) return cfg.rotation.enableLOS;
                    break;
                case "etherwarp":
                    if ("enableOvershoot".equals(parts[1])) return cfg.etherwarp.enableOvershoot;
                    break;
            }
        } catch (Exception e) {
            WieselClient.LOGGER.error("Error getting config value: {}", path, e);
        }
        return defaultValue;
    }
}
