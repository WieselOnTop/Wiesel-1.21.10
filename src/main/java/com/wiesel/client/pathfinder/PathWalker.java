package com.wiesel.client.pathfinder;

import com.wiesel.client.WieselClient;
import com.wiesel.client.rotation.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class PathWalker {
    private static List<PathNode> currentPath = null;
    private static int currentNodeIndex = 0;
    private static boolean isWalking = false;
    private static final double REACH_THRESHOLD = 1.0; // Distance to consider node reached
    private static final RotationManager rotationManager = new RotationManager();

    public static void startWalking(PathfindResponse path) {
        if (path == null || path.path == null || path.path.isEmpty()) {
            WieselClient.LOGGER.warn("Cannot start walking: invalid path");
            return;
        }

        currentPath = path.path;
        currentNodeIndex = 0;
        isWalking = true;
        rotationManager.setPath(path.path);
        WieselClient.LOGGER.info("Started walking path with {} nodes", currentPath.size());
    }

    public static void stopWalking() {
        isWalking = false;
        currentPath = null;
        currentNodeIndex = 0;
        rotationManager.stop();
        WieselClient.LOGGER.info("Stopped walking");
    }

    public static void tick() {
        if (!isWalking || currentPath == null || currentPath.isEmpty()) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        if (player == null) {
            stopWalking();
            return;
        }

        // Check if we've reached the end
        if (currentNodeIndex >= currentPath.size()) {
            WieselClient.LOGGER.info("Reached end of path");
            stopWalking();
            return;
        }

        PathNode targetNode = currentPath.get(currentNodeIndex);
        Vec3d playerPos = player.getPos();
        Vec3d targetPos = new Vec3d(targetNode.x + 0.5, targetNode.y, targetNode.z + 0.5);

        double distance = playerPos.distanceTo(targetPos);

        // Check if we reached the current node
        if (distance < REACH_THRESHOLD) {
            currentNodeIndex++;
            rotationManager.setCurrentNodeIndex(currentNodeIndex);
            if (currentNodeIndex < currentPath.size()) {
                WieselClient.LOGGER.debug("Reached node {}/{}", currentNodeIndex, currentPath.size());
                targetNode = currentPath.get(currentNodeIndex);
                targetPos = new Vec3d(targetNode.x + 0.5, targetNode.y, targetNode.z + 0.5);
            } else {
                stopWalking();
                return;
            }
        }

        // Rotation is handled by RotationManager in the render event
        // Move forward
        player.input.pressingForward = true;
        player.input.movementForward = 1.0f;

        // Jump if needed (simple jump detection)
        if (targetPos.y > playerPos.y + 0.5) {
            player.input.jumping = true;
        }
    }

    public static RotationManager getRotationManager() {
        return rotationManager;
    }

    public static boolean isWalking() {
        return isWalking;
    }

    public static int getCurrentNodeIndex() {
        return currentNodeIndex;
    }

    public static int getTotalNodes() {
        return currentPath != null ? currentPath.size() : 0;
    }
}
