package com.wiesel.client.pathfinder;

import com.wiesel.client.WieselClient;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class PathfinderManager {
    private static Process pathfinderProcess = null;
    private static final File pathfinderExecutable;

    static {
        pathfinderExecutable = extractPathfinderExecutable();
    }

    private static File extractPathfinderExecutable() {
        String osName = System.getProperty("os.name").toLowerCase();
        String resourcePath = osName.contains("win") ? "/natives/pathfinder.exe"
                : osName.contains("mac") ? "/natives/pathfinder-mac"
                : "/natives/pathfinder-linux";

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "wiesel-client");
        tempDir.mkdirs();

        File executableFile = new File(tempDir, "pathfinder" + (osName.contains("win") ? ".exe" : ""));

        try {
            InputStream inputStream = PathfinderManager.class.getResourceAsStream(resourcePath);
            if (inputStream != null) {
                Files.copy(inputStream, executableFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                executableFile.setExecutable(true);
                WieselClient.LOGGER.info("Extracted pathfinder executable to: {}", executableFile.getAbsolutePath());
            } else {
                WieselClient.LOGGER.warn("Pathfinder executable not found in resources: {}", resourcePath);
            }
        } catch (Exception e) {
            WieselClient.LOGGER.error("Failed to extract pathfinder executable", e);
        }

        return executableFile;
    }

    public static void startPathfinder(List<String> args) {
        if (!pathfinderExecutable.exists()) {
            WieselClient.LOGGER.error("Pathfinder executable not found");
            return;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(pathfinderExecutable.getAbsolutePath());
            if (args != null && !args.isEmpty()) {
                processBuilder.command().addAll(args);
            }
            pathfinderProcess = processBuilder.start();
            WieselClient.LOGGER.info("Started pathfinder process");
        } catch (Exception e) {
            WieselClient.LOGGER.error("Failed to start pathfinder", e);
        }
    }

    public static void stopPathfinder() {
        if (pathfinderProcess != null) {
            pathfinderProcess.destroy();
            pathfinderProcess = null;
            WieselClient.LOGGER.info("Stopped pathfinder process");
        }
    }
}
