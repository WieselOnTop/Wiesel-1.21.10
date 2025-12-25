package com.wiesel.client.rotation;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Provides humanized rotation calculations with jitter, offsets, and natural movement
 * Used for tree breaking and etherwarp to look more human-like
 */
public class HumanizedRotation {

    private static final Random random = new Random();

    // Humanization parameters
    private static final double CENTER_OFFSET_RANGE = 0.35; // Max offset from block center (blocks)
    private static final double ROTATION_JITTER = 0.8; // Random jitter in degrees
    private static final double OVERSHOOT_CHANCE = 0.2; // 20% chance to overshoot
    private static final double OVERSHOOT_AMOUNT = 1.5; // Max overshoot in degrees
    private static final double DRIFT_AMOUNT = 0.3; // Continuous drift while mining (degrees)
    private static final double ROTATION_SMOOTHNESS = 0.45; // How smooth rotations are (0=instant, 1=very smooth) - faster, more responsive

    /**
     * Target rotation data
     */
    public static class RotationTarget {
        public final float yaw;
        public final float pitch;
        public final Vec3d targetPoint; // The exact point we're looking at

        public RotationTarget(float yaw, float pitch, Vec3d targetPoint) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.targetPoint = targetPoint;
        }
    }

    /**
     * Calculate humanized rotation to look at a block (with offset from center)
     * @param eyePos Player's eye position
     * @param targetBlock Block to look at
     * @param addOffset Whether to add random offset from center
     * @return Humanized rotation target
     */
    public static RotationTarget getHumanizedRotation(Vec3d eyePos, BlockPos targetBlock, boolean addOffset) {
        // Calculate center of block
        double centerX = targetBlock.getX() + 0.5;
        double centerY = targetBlock.getY() + 0.5;
        double centerZ = targetBlock.getZ() + 0.5;

        // Add random offset from center to make it look more human
        if (addOffset) {
            centerX += (random.nextDouble() - 0.5) * CENTER_OFFSET_RANGE;
            centerY += (random.nextDouble() - 0.5) * CENTER_OFFSET_RANGE;
            centerZ += (random.nextDouble() - 0.5) * CENTER_OFFSET_RANGE;
        }

        Vec3d targetPoint = new Vec3d(centerX, centerY, centerZ);

        // Calculate rotation to target
        double deltaX = targetPoint.x - eyePos.x;
        double deltaY = targetPoint.y - eyePos.y;
        double deltaZ = targetPoint.z - eyePos.z;

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

        // Add random jitter
        yaw += (float) ((random.nextDouble() - 0.5) * ROTATION_JITTER);
        pitch += (float) ((random.nextDouble() - 0.5) * ROTATION_JITTER);

        // Occasionally overshoot the target
        if (random.nextDouble() < OVERSHOOT_CHANCE) {
            yaw += (float) ((random.nextDouble() - 0.5) * OVERSHOOT_AMOUNT * 2);
            pitch += (float) ((random.nextDouble() - 0.5) * OVERSHOOT_AMOUNT * 2);
        }

        // Normalize angles
        yaw = normalizeYaw(yaw);
        pitch = clampPitch(pitch);

        return new RotationTarget(yaw, pitch, targetPoint);
    }

    /**
     * Apply smooth rotation interpolation (human-like smooth movement)
     * @param currentYaw Current yaw
     * @param currentPitch Current pitch
     * @param targetYaw Target yaw
     * @param targetPitch Target pitch
     * @param smoothness Smoothness factor (0-1)
     * @return Interpolated rotation
     */
    public static RotationTarget smoothRotation(float currentYaw, float currentPitch,
                                                float targetYaw, float targetPitch,
                                                float smoothness) {
        // Calculate shortest path for yaw (handle 360 degree wraparound)
        float yawDiff = normalizeYaw(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        // Apply smoothing
        float newYaw = currentYaw + yawDiff * smoothness;
        float newPitch = currentPitch + pitchDiff * smoothness;

        // Normalize
        newYaw = normalizeYaw(newYaw);
        newPitch = clampPitch(newPitch);

        return new RotationTarget(newYaw, newPitch, null);
    }

    /**
     * Add natural drift to rotation (simulates hand movement while mining)
     * @param currentYaw Current yaw
     * @param currentPitch Current pitch
     * @return Slightly drifted rotation
     */
    public static RotationTarget addDrift(float currentYaw, float currentPitch) {
        float driftYaw = (float) ((random.nextDouble() - 0.5) * DRIFT_AMOUNT);
        float driftPitch = (float) ((random.nextDouble() - 0.5) * DRIFT_AMOUNT);

        float newYaw = normalizeYaw(currentYaw + driftYaw);
        float newPitch = clampPitch(currentPitch + driftPitch);

        return new RotationTarget(newYaw, newPitch, null);
    }

    /**
     * Calculate rotation to a specific Vec3d point
     * @param eyePos Player's eye position
     * @param target Target position
     * @return Rotation to target
     */
    public static RotationTarget getRotationToVec(Vec3d eyePos, Vec3d target) {
        double deltaX = target.x - eyePos.x;
        double deltaY = target.y - eyePos.y;
        double deltaZ = target.z - eyePos.z;

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

        yaw = normalizeYaw(yaw);
        pitch = clampPitch(pitch);

        return new RotationTarget(yaw, pitch, target);
    }

    /**
     * Normalize yaw to -180 to 180 range
     */
    private static float normalizeYaw(float yaw) {
        yaw = yaw % 360.0f;
        if (yaw >= 180.0f) {
            yaw -= 360.0f;
        }
        if (yaw < -180.0f) {
            yaw += 360.0f;
        }
        return yaw;
    }

    /**
     * Clamp pitch to -90 to 90 range
     */
    private static float clampPitch(float pitch) {
        if (pitch > 90.0f) {
            return 90.0f;
        }
        if (pitch < -90.0f) {
            return -90.0f;
        }
        return pitch;
    }

    /**
     * Get current rotation smoothness setting
     */
    public static double getRotationSmoothness() {
        return ROTATION_SMOOTHNESS;
    }

    /**
     * Calculate if we're looking close enough at the target
     * @param currentYaw Current yaw
     * @param currentPitch Current pitch
     * @param target Target rotation
     * @param tolerance Tolerance in degrees
     * @return True if within tolerance
     */
    public static boolean isLookingAt(float currentYaw, float currentPitch,
                                     RotationTarget target, float tolerance) {
        float yawDiff = Math.abs(normalizeYaw(target.yaw - currentYaw));
        float pitchDiff = Math.abs(target.pitch - currentPitch);

        return yawDiff <= tolerance && pitchDiff <= tolerance;
    }
}
