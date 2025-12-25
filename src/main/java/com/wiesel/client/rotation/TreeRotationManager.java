package com.wiesel.client.rotation;

import com.wiesel.client.WieselClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Tree breaking rotation system based on RotationManager.
 * Uses time-based smooth rotations with human-like behavior.
 * Copied from RotationManager and adapted for tree breaking.
 */
public class TreeRotationManager {
    private static final Random random = new Random();

    // Base rotation speed (degrees per second) - balanced for smooth natural look
    private static final float BASE_ROTATION_SPEED = 9.5f; // Slightly faster than 8.5 but still natural

    // Humanization parameters - BALANCED for smooth natural look
    private static final double CENTER_OFFSET_RANGE = 0.20; // Moderate offset - not too perfect, not too wild
    private static final float OVERSHOOT_CHANCE = 0.12f; // Occasional overshoots
    private static final float OVERSHOOT_AMOUNT = 0.8f; // Subtle overshoot
    private static final float DRIFT_AMOUNT = 0.04f; // Minimal drift while mining

    // Micro jitter - MINIMAL for smooth look
    private static final int JITTER_UPDATE_FRAMES = 8; // Less frequent updates for smoother motion
    private static final float MICRO_JITTER_AMOUNT = 0.08f; // Subtle jitter, not too jerky

    // State
    private BlockPos targetBlock = null;
    private Vec3d targetPoint = null;
    private boolean isActive = false;

    // Smooth rotation state
    private float smoothYaw = 0;
    private float smoothPitch = 0;

    // Timing
    private long lastRenderTime = System.currentTimeMillis();

    // Target angles
    private float targetYaw = 0;
    private float targetPitch = 0;

    // Human-like behavior (from RotationManager)
    private float overshootYaw = 0;
    private float overshootPitch = 0;
    private boolean hasOvershot = false;
    private float speedVariance = 1.0f;
    private int speedPattern = 0;

    // Micro jitter
    private float microJitterYaw = 0;
    private float microJitterPitch = 0;
    private int jitterFrameCount = 0;

    // Retargeting (add variation to avoid stale targeting)
    private int framesSinceRetarget = 0;
    private int retargetInterval = 30; // Retarget every 30-50 frames (0.5-0.8 seconds) - more frequent for dynamic look

    // Speed patterns (from RotationManager)
    private enum SpeedPattern {
        SMOOTH,      // Smooth consistent movement
        QUICK_START, // Fast start, gradual slowdown
        ACCELERATE,  // Slow start, fast middle, slow end
        STEADY,      // Very consistent
        BURST        // Quick bursts
    }

    /**
     * Start looking at a block for tree breaking
     * @param block Block to look at
     * @param addOffset Whether to add random offset from center (for variation)
     */
    public void lookAtBlock(BlockPos block, boolean addOffset) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        this.targetBlock = block;
        this.isActive = true;
        this.framesSinceRetarget = 0;
        this.retargetInterval = 30 + random.nextInt(21); // 30-50 frames (0.5-0.8 seconds)

        // Initialize from current rotation
        this.smoothYaw = client.player.getYaw();
        this.smoothPitch = client.player.getPitch();

        // Calculate target point with optional offset (from HumanizedRotation)
        double centerX = block.getX() + 0.5;
        double centerY = block.getY() + 0.5;
        double centerZ = block.getZ() + 0.5;

        if (addOffset) {
            centerX += (random.nextDouble() - 0.5) * CENTER_OFFSET_RANGE;
            centerY += (random.nextDouble() - 0.5) * CENTER_OFFSET_RANGE;
            centerZ += (random.nextDouble() - 0.5) * CENTER_OFFSET_RANGE;
        }

        this.targetPoint = new Vec3d(centerX, centerY, centerZ);

        // Calculate target angles
        updateTargetAngles(client.player);

        // Human-like overshoot behavior - more pronounced
        if (random.nextFloat() < OVERSHOOT_CHANCE) {
            float overshootMult = 0.4f + random.nextFloat() * 0.9f; // 0.4-1.3x variation
            float overshootAngle = random.nextFloat() * (float) (2 * Math.PI);
            this.overshootYaw = (float) (Math.cos(overshootAngle) * OVERSHOOT_AMOUNT * overshootMult);
            this.overshootPitch = (float) (Math.sin(overshootAngle) * OVERSHOOT_AMOUNT * overshootMult * 0.5f);
        } else {
            this.overshootYaw = 0;
            this.overshootPitch = 0;
        }
        this.hasOvershot = false;

        // Random speed variance - BALANCED variation for human look
        this.speedVariance = 0.80f + random.nextFloat() * 0.35f; // 0.80-1.15x (moderate range)

        // Select random speed pattern (from RotationManager)
        this.speedPattern = random.nextInt(5); // 0-4
    }

    /**
     * Update target angles based on current player position
     * Uses actual eye position to account for jumping/crouching
     */
    private void updateTargetAngles(ClientPlayerEntity player) {
        // Use actual eye position - this automatically accounts for jumping, crouching, etc.
        Vec3d playerEyes = player.getEyePos();

        double dx = targetPoint.x - playerEyes.x;
        double dy = targetPoint.y - playerEyes.y;
        double dz = targetPoint.z - playerEyes.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        this.targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        this.targetPitch = (float) -(Math.atan2(dy, distXZ) * 180.0 / Math.PI);
        this.targetYaw = MathHelper.wrapDegrees(this.targetYaw);
        this.targetPitch = MathHelper.clamp(this.targetPitch, -90, 90);
    }

    /**
     * Stop looking at target
     */
    public void stop() {
        isActive = false;
        targetBlock = null;
        targetPoint = null;
        hasOvershot = false;
        overshootYaw = 0;
        overshootPitch = 0;
        microJitterYaw = 0;
        microJitterPitch = 0;
        jitterFrameCount = 0;
        framesSinceRetarget = 0;
    }

    /**
     * Called every render frame for smooth rotation.
     * This is the main method that should be called from the render loop.
     */
    public void onRender() {
        if (!isActive) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        ClientPlayerEntity player = client.player;
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderTime) / 1000.0f, 0.05f); // Max 50ms delta
        lastRenderTime = now;

        if (dt <= 0) return;

        // Retarget periodically for variation (like HumanizedRotation retargeting)
        framesSinceRetarget++;
        if (framesSinceRetarget >= retargetInterval) {
            // Add new random offset to target
            double centerX = targetBlock.getX() + 0.5;
            double centerY = targetBlock.getY() + 0.5;
            double centerZ = targetBlock.getZ() + 0.5;

            centerX += (random.nextDouble() - 0.5) * CENTER_OFFSET_RANGE;
            centerY += (random.nextDouble() - 0.5) * CENTER_OFFSET_RANGE;
            centerZ += (random.nextDouble() - 0.5) * CENTER_OFFSET_RANGE;

            this.targetPoint = new Vec3d(centerX, centerY, centerZ);
            updateTargetAngles(player);

            framesSinceRetarget = 0;
            retargetInterval = 30 + random.nextInt(21); // Next retarget in 30-50 frames
        }

        // Update target angles every frame based on current player position
        // The targetPoint stays the same, but angles to it change when player moves/jumps
        updateTargetAngles(player);

        // Determine effective target with overshoot behavior (from RotationManager)
        float effectiveTargetYaw = targetYaw;
        float effectiveTargetPitch = targetPitch;

        if (!hasOvershot && (overshootYaw != 0 || overshootPitch != 0)) {
            effectiveTargetYaw = targetYaw + overshootYaw;
            effectiveTargetPitch = targetPitch + overshootPitch;
        }

        float yawDiff = angleDiff(smoothYaw, effectiveTargetYaw);
        float pitchDiff = effectiveTargetPitch - smoothPitch;
        float absYawDiff = Math.abs(yawDiff);
        float absPitchDiff = Math.abs(pitchDiff);

        // Check if overshoot phase is complete (from RotationManager)
        if (!hasOvershot && (overshootYaw != 0 || overshootPitch != 0)) {
            if (absYawDiff < 0.5f && absPitchDiff < 0.4f) {
                hasOvershot = true;
            }
        }

        // Update micro jitter (from RotationManager)
        jitterFrameCount++;
        if (jitterFrameCount >= JITTER_UPDATE_FRAMES) {
            jitterFrameCount = 0;

            float distToTarget = Math.abs(angleDiff(smoothYaw, targetYaw)) + Math.abs(targetPitch - smoothPitch);

            // Disable jitter when very close for precision
            if (distToTarget < 1.5f) {
                microJitterYaw = 0;
                microJitterPitch = 0;
            } else {
                // Add drift when actively breaking (from HumanizedRotation DRIFT_AMOUNT)
                float driftMult = 1.0f;
                if (distToTarget < 3.0f) {
                    driftMult = 1.0f + DRIFT_AMOUNT; // More drift when close
                }

                microJitterYaw = (float) ((random.nextDouble() - 0.5) * 2.0 * MICRO_JITTER_AMOUNT * driftMult);
                microJitterPitch = (float) ((random.nextDouble() - 0.5) * 2.0 * MICRO_JITTER_AMOUNT * driftMult);
            }
        }

        // Base speed with variance (from RotationManager)
        float baseYawSpeed = BASE_ROTATION_SPEED * speedVariance;
        float basePitchSpeed = (BASE_ROTATION_SPEED * 0.88f) * speedVariance;

        // Get pattern multipliers (from RotationManager)
        float yawSpeedMult = getPatternSpeedMultiplier(speedPattern, absYawDiff, true);
        float pitchSpeedMult = getPatternSpeedMultiplier(speedPattern, absPitchDiff, false);

        float yawSpeed = baseYawSpeed * yawSpeedMult;
        float pitchSpeed = basePitchSpeed * pitchSpeedMult;

        // Exponential decay interpolation (from RotationManager)
        float yawAlpha = 1.0f - (float) Math.exp(-yawSpeed * dt);
        float pitchAlpha = 1.0f - (float) Math.exp(-pitchSpeed * dt);

        // Apply rotation incrementally
        smoothYaw += yawDiff * yawAlpha;
        smoothYaw = normalizeAngle(smoothYaw);

        smoothPitch += pitchDiff * pitchAlpha;
        smoothPitch = MathHelper.clamp(smoothPitch, -90, 90);

        // Apply micro jitter
        float finalYaw = smoothYaw + microJitterYaw;
        float finalPitch = smoothPitch + microJitterPitch;

        finalYaw = normalizeAngle(finalYaw);
        finalPitch = MathHelper.clamp(finalPitch, -90, 90);

        // Apply to player
        player.setYaw(finalYaw);
        player.setPitch(finalPitch);
    }

    /**
     * Get speed multiplier based on pattern and distance (from RotationManager)
     */
    private float getPatternSpeedMultiplier(int pattern, float distance, boolean isYaw) {
        float farThreshold = isYaw ? 30.0f : 20.0f;
        float midThreshold = isYaw ? 15.0f : 10.0f;
        float nearThreshold = isYaw ? 5.0f : 4.0f;

        switch (pattern) {
            case 0: // SMOOTH
                if (distance > farThreshold) return 1.15f;
                if (distance > midThreshold) return 1.1f;
                if (distance < nearThreshold) return 0.75f;
                return 1.0f;

            case 1: // QUICK_START
                if (distance > farThreshold) return 1.35f;
                if (distance > midThreshold) return 1.15f;
                if (distance > nearThreshold) return 0.9f;
                return 0.7f;

            case 2: // ACCELERATE
                if (distance > farThreshold) return 0.9f;
                if (distance > midThreshold) return 1.3f;
                if (distance < nearThreshold) return 0.75f;
                return 1.05f;

            case 3: // STEADY
                if (distance > farThreshold) return 1.05f;
                if (distance < nearThreshold) return 0.95f;
                return 1.0f;

            case 4: // BURST
                if (distance > farThreshold) return 1.25f;
                if (distance > midThreshold) return 1.35f;
                if (distance > nearThreshold) return 1.1f;
                return 0.8f;

            default:
                return 1.0f;
        }
    }

    /**
     * Check if currently looking at the target block (within tolerance)
     */
    public boolean isLookingAtTarget(float tolerance) {
        if (!isActive) return false;

        float yawDiff = Math.abs(angleDiff(smoothYaw, targetYaw));
        float pitchDiff = Math.abs(targetPitch - smoothPitch);

        return yawDiff <= tolerance && pitchDiff <= tolerance;
    }

    /**
     * Get current smooth yaw
     */
    public float getYaw() {
        return smoothYaw;
    }

    /**
     * Get current smooth pitch
     */
    public float getPitch() {
        return smoothPitch;
    }

    /**
     * Check if rotation system is active
     */
    public boolean isActive() {
        return isActive;
    }

    private float angleDiff(float from, float to) {
        float diff = normalizeAngle(to - from);
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        return diff;
    }

    private float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }
}
