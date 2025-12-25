package com.wiesel.client.pathfinder;

import com.wiesel.client.WieselClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class PathWalker {
    private static List<PathNode> currentPath = null;
    private static int currentNodeIndex = 0;
    private static boolean isWalking = false;
    private static final double REACH_THRESHOLD = 1.0; // Distance to consider node reached

    public static void startWalking(PathfindResponse path) {
        if (path == null || path.path == null || path.path.isEmpty()) {
            WieselClient.LOGGER.warn("Cannot start walking: invalid path");
            return;
        }

        currentPath = path.path;
        currentNodeIndex = 0;
        isWalking = true;
        WieselClient.LOGGER.info("Started walking path with {} nodes", currentPath.size());
    }

    public static void stopWalking() {
        isWalking = false;
        currentPath = null;
        currentNodeIndex = 0;
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
            if (currentNodeIndex < currentPath.size()) {
                WieselClient.LOGGER.debug("Reached node {}/{}", currentNodeIndex, currentPath.size());
                targetNode = currentPath.get(currentNodeIndex);
                targetPos = new Vec3d(targetNode.x + 0.5, targetNode.y, targetNode.z + 0.5);
            } else {
                stopWalking();
                return;
            }
        }

        // Calculate rotation to target
        rotateTo(player, targetPos);

        // Move forward
        player.input.pressingForward = true;
        player.input.movementForward = 1.0f;

        // Jump if needed (simple jump detection)
        if (targetPos.y > playerPos.y + 0.5) {
            player.input.jumping = true;
        }
    }

    private static void rotateTo(ClientPlayerEntity player, Vec3d target) {
        Vec3d playerPos = player.getPos();
        Vec3d eyePos = playerPos.add(0, player.getEyeHeight(player.getPose()), 0);

        double deltaX = target.x - eyePos.x;
        double deltaY = target.y - eyePos.y;
        double deltaZ = target.z - eyePos.z;

        double distanceXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Calculate yaw (horizontal rotation)
        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;

        // Calculate pitch (vertical rotation)
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, distanceXZ));

        // Normalize angles
        yaw = MathHelper.wrapDegrees(yaw);
        pitch = MathHelper.clamp(pitch, -90.0f, 90.0f);

        // Set player rotation
        player.setYaw(yaw);
        player.setPitch(pitch);
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
