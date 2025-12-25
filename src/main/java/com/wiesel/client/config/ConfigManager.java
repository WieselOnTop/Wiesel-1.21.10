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
}
