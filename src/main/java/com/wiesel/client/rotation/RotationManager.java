package com.wiesel.client.rotation;

import com.wiesel.client.WieselClient;
import com.wiesel.client.pathfinder.PathNode;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Human-like rotation system with predictive pitch and lookahead.
 * Ported from ChatTriggers HumanRotation.js
 * Uses render events for smooth 60fps+ rotation.
 */
public class RotationManager {
    // Lookahead settings (loaded from config)
    private int lookaheadNodes = 8;
    private double lookaheadMinDist = 4.0;
    private double lookaheadMaxDist = 15.0;
    private boolean enableLOS = true;

    // Rotation speeds (loaded from config)
    private float yawSpeed = 7.0f;
    private float pitchSpeed = 4.5f;

    // Etherwarp rotation defaults (will be loaded from config)
    private static final float DEFAULT_ETHERWARP_SPEED = 8.5f;
    private static final float ETHERWARP_COMPLETION_THRESHOLD = 0.35f; // Within 0.35 degrees for maximum precision

    // Corner handling (loaded from config)
    private float cornerBoost = 1.5f;
    private static final float CORNER_THRESHOLD = 25.0f;

    /**
     * Load rotation settings from config.
     */
    private void loadConfig() {
        yawSpeed = WieselClient.getInstance().getConfigManager().getFloat("rotation.yawSpeed", 7.0f);
        pitchSpeed = WieselClient.getInstance().getConfigManager().getFloat("rotation.pitchSpeed", 4.5f);
        lookaheadNodes = (int) WieselClient.getInstance().getConfigManager().getFloat("rotation.lookahead", 8f);
        lookaheadMinDist = WieselClient.getInstance().getConfigManager().getFloat("rotation.lookaheadMinDist", 4.0f);
        lookaheadMaxDist = WieselClient.getInstance().getConfigManager().getFloat("rotation.lookaheadMaxDist", 15.0f);
        enableLOS = WieselClient.getInstance().getConfigManager().getBoolean("rotation.enableLOS", true);
        cornerBoost = WieselClient.getInstance().getConfigManager().getFloat("rotation.cornerBoost", 1.5f);
    }

    // Predictive pitch
    private static final float BASE_PITCH = 8.0f;      // Slight downward look
    private static final float CLIMB_PITCH = -15.0f;   // Look up when climbing
    private static final float DESCEND_PITCH = 20.0f;  // Look down when descending
    private static final int PREDICTION_NODES = 6;

    // State
    private List<PathNode> path;
    private int currentNodeIndex = 0;
    private boolean isActive = false;

    // Smooth rotation state
    private float smoothYaw = 0;
    private float smoothPitch = 0;

    // Timing
    private long lastRenderTime = System.currentTimeMillis();

    // Target mode (for etherwarp, looking at specific block)
    private Vec3d targetPoint = null;
    private boolean targetMode = false;
    private boolean targetWhileSneaking = false; // Use sneaking eye height for calculations
    private Runnable onTargetReached = null;
    private float targetYaw = 0;
    private float targetPitch = 0;
    private long targetStartTime = 0;
    private static final long TARGET_TIMEOUT_MS = 2000; // 2 second timeout

    // Human-like behavior
    private float overshootYaw = 0;    // Random overshoot amount
    private float overshootPitch = 0;  // Random overshoot amount
    private boolean hasOvershot = false;  // Track if we've done the overshoot phase
    private float speedVariance = 1.0f;   // Random speed multiplier
    private int speedPattern = 0;         // Which hardcoded speed pattern to use (0-4)

    // Micro jitter (human hand instability)
    private float microJitterYaw = 0;     // Current micro jitter offset for yaw
    private float microJitterPitch = 0;   // Current micro jitter offset for pitch
    private int jitterFrameCount = 0;     // Frames since last jitter update
    private static final int JITTER_UPDATE_FRAMES = 3; // Update jitter every N frames
    private static final float MICRO_JITTER_AMOUNT = 0.15f; // ±0.15 degrees of jitter

    // Hardcoded speed patterns (all multiply the config base speed)
    private enum SpeedPattern {
        SMOOTH,      // Smooth consistent movement with slight slowdown
        QUICK_START, // Fast start, gradual slowdown
        ACCELERATE,  // Slow start, fast middle, slow end
        STEADY,      // Very consistent with micro-variations
        BURST        // Quick bursts with slight variations
    }

    // Eye heights (Minecraft 1.14+ with new sneaking animation)
    private static final double STANDING_EYE_HEIGHT = 1.62;
    private static final double SNEAKING_EYE_HEIGHT = 1.32; // 1.5 (sneak height) - 0.18

    public void setPath(List<PathNode> newPath) {
        this.path = newPath;
        this.currentNodeIndex = 0;
        this.isActive = true;
        this.targetMode = false;
        this.targetPoint = null;

        // Load config values
        loadConfig();

        // Initialize rotations from player
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            this.smoothYaw = client.player.getYaw();
            this.smoothPitch = client.player.getPitch();
        }
    }

    /**
     * Look at a specific target point (for etherwarp, etc).
     * @param target The point to look at
     * @param whileSneaking If true, calculate angles using sneaking eye height
     * @param onComplete Callback when rotation is complete (close enough to target)
     */
    public void lookAt(Vec3d target, boolean whileSneaking, Runnable onComplete) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        this.targetPoint = target;
        this.targetMode = true;
        this.targetWhileSneaking = whileSneaking;
        this.isActive = true;
        this.onTargetReached = onComplete;
        this.path = null;
        this.targetStartTime = System.currentTimeMillis();

        // Initialize from current rotation
        this.smoothYaw = client.player.getYaw();
        this.smoothPitch = client.player.getPitch();

        // Calculate target angles using appropriate eye height
        double eyeHeight = whileSneaking ? SNEAKING_EYE_HEIGHT : STANDING_EYE_HEIGHT;
        Vec3d playerPos = client.player.getPos();
        Vec3d playerEyes = playerPos.add(0, eyeHeight, 0);

        double dx = target.x - playerEyes.x;
        double dy = target.y - playerEyes.y;
        double dz = target.z - playerEyes.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        this.targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        this.targetPitch = (float) -(Math.atan2(dy, distXZ) * 180.0 / Math.PI);
        this.targetYaw = MathHelper.wrapDegrees(this.targetYaw);
        this.targetPitch = MathHelper.clamp(this.targetPitch, -90, 90);

        // Load config values
        float configOvershootAmount = WieselClient.getInstance().getConfigManager()
            .getFloat("etherwarp.overshootAmount", 1.5f);
        boolean enableOvershoot = WieselClient.getInstance().getConfigManager()
            .getBoolean("etherwarp.enableOvershoot", true);
        float speedVariation = WieselClient.getInstance().getConfigManager()
            .getFloat("etherwarp.speedVariation", 0.3f);

        // Human-like overshoot behavior
        if (enableOvershoot) {
            // 70% chance to overshoot, varies by 0.6x-1.3x of config amount
            if (Math.random() < 0.7) {
                float overshootMult = 0.6f + (float) (Math.random() * 0.7);
                // Overshoot in a random direction around target
                float overshootAngle = (float) (Math.random() * 2 * Math.PI);
                this.overshootYaw = (float) (Math.cos(overshootAngle) * configOvershootAmount * overshootMult);
                this.overshootPitch = (float) (Math.sin(overshootAngle) * configOvershootAmount * overshootMult * 0.6f); // Less pitch overshoot
                WieselClient.LOGGER.info("[RotationManager] Overshoot enabled: yaw={}, pitch={}",
                    String.format("%.2f", overshootYaw), String.format("%.2f", overshootPitch));
            } else {
                this.overshootYaw = 0;
                this.overshootPitch = 0;
            }
        } else {
            this.overshootYaw = 0;
            this.overshootPitch = 0;
        }
        this.hasOvershot = false;

        // Random speed variance based on config (small variations from base)
        float baseVariance = 1.0f - speedVariation * 0.5f;
        this.speedVariance = baseVariance + (float) (Math.random() * speedVariation);

        // Select random hardcoded speed pattern
        this.speedPattern = (int) (Math.random() * 5); // 0-4

        WieselClient.LOGGER.info("[RotationManager] lookAt started: target=({}, {}, {}), yaw={}, pitch={}, pattern={}",
            String.format("%.1f", target.x), String.format("%.1f", target.y), String.format("%.1f", target.z),
            String.format("%.1f", this.targetYaw), String.format("%.1f", this.targetPitch),
            SpeedPattern.values()[speedPattern].name());
    }

    /**
     * Look at a specific target point (standing eye height).
     */
    public void lookAt(Vec3d target, Runnable onComplete) {
        lookAt(target, false, onComplete);
    }

    /**
     * Look at a block position (aims at center of block's top face).
     * For etherwarp, we aim slightly below the top surface to ensure the raycast hits the block.
     * @param pos Block position to look at
     * @param whileSneaking If true, calculate angles using sneaking eye height (for etherwarp)
     * @param onComplete Callback when rotation is complete
     */
    public void lookAtBlock(BlockPos pos, boolean whileSneaking, Runnable onComplete) {
        // Calculate the optimal aiming point based on player position
        Vec3d target = calculateOptimalBlockTarget(pos, whileSneaking);
        lookAt(target, whileSneaking, onComplete);
    }

    /**
     * Calculate the optimal point to aim at on a block surface.
     * Avoids edges and ensures the raycast will hit the block reliably.
     * Adds slight randomization for human-like behavior.
     */
    private Vec3d calculateOptimalBlockTarget(BlockPos blockPos, boolean whileSneaking) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            // Fallback to simple center
            return new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.9, blockPos.getZ() + 0.5);
        }

        double eyeHeight = whileSneaking ? SNEAKING_EYE_HEIGHT : STANDING_EYE_HEIGHT;
        Vec3d playerEyes = client.player.getPos().add(0, eyeHeight, 0);

        // Determine which face of the block to aim at based on player position
        double blockCenterX = blockPos.getX() + 0.5;
        double blockCenterY = blockPos.getY() + 0.5;
        double blockCenterZ = blockPos.getZ() + 0.5;

        double dx = blockCenterX - playerEyes.x;
        double dy = blockCenterY - playerEyes.y;
        double dz = blockCenterZ - playerEyes.z;

        // Calculate which face is most directly visible
        double absDx = Math.abs(dx);
        double absDy = Math.abs(dy);
        double absDz = Math.abs(dz);

        // Target point with safe margins from edges (0.3-0.7 instead of 0-1)
        double targetX = blockCenterX;
        double targetY = blockCenterY;
        double targetZ = blockCenterZ;

        // For etherwarp, aim at the optimal Y height based on player position
        // Always stay well within block bounds
        if (playerEyes.y < blockPos.getY() + 0.3) {
            // Player is below block - aim at mid-upper face for reliability
            targetY = blockPos.getY() + 0.75;
        } else if (playerEyes.y > blockPos.getY() + 1.8) {
            // Player is above block - aim at upper-middle
            targetY = blockPos.getY() + 0.65;
        } else {
            // Player is at block level - aim at safe mid-upper area
            targetY = blockPos.getY() + 0.75;
        }

        // Very minimal offset calculation for visibility
        // Stay very close to center for maximum reliability
        double xOffset = 0;
        double zOffset = 0;

        // Only apply tiny offset if player is significantly offset
        if (absDx > 1.0) {
            xOffset = MathHelper.clamp(-dx / absDx * 0.05, -0.08, 0.08);
        }
        if (absDz > 1.0) {
            zOffset = MathHelper.clamp(-dz / absDz * 0.05, -0.08, 0.08);
        }

        // NO randomization for maximum reliability
        // Human-like feel comes from rotation movement, not target variance
        targetX = blockCenterX + xOffset;
        targetY = targetY;
        targetZ = blockCenterZ + zOffset;

        // Very tight clamping to dead center region
        targetX = MathHelper.clamp(targetX, blockPos.getX() + 0.4, blockPos.getX() + 0.6);
        targetY = MathHelper.clamp(targetY, blockPos.getY() + 0.65, blockPos.getY() + 0.82);
        targetZ = MathHelper.clamp(targetZ, blockPos.getZ() + 0.4, blockPos.getZ() + 0.6);

        return new Vec3d(targetX, targetY, targetZ);
    }

    /**
     * Look at a block position (standing eye height).
     */
    public void lookAtBlock(BlockPos pos, Runnable onComplete) {
        lookAtBlock(pos, false, onComplete);
    }

    /**
     * Check if currently looking at a target (not path following).
     */
    public boolean isLookingAtTarget() {
        return targetMode && isActive;
    }

    /**
     * Cancel target look mode.
     */
    public void cancelTargetLook() {
        if (targetMode) {
            targetMode = false;
            targetWhileSneaking = false;
            targetPoint = null;
            onTargetReached = null;
            isActive = false;
            hasOvershot = false;
            overshootYaw = 0;
            overshootPitch = 0;
            speedPattern = 0;
            microJitterYaw = 0;
            microJitterPitch = 0;
            jitterFrameCount = 0;
        }
    }

    public void setCurrentNodeIndex(int index) {
        this.currentNodeIndex = index;
    }

    public void stop() {
        isActive = false;
        path = null;
        currentNodeIndex = 0;
        targetMode = false;
        targetWhileSneaking = false;
        targetPoint = null;
        onTargetReached = null;
        hasOvershot = false;
        overshootYaw = 0;
        overshootPitch = 0;
        speedVariance = 1.0f;
        speedPattern = 0;
        microJitterYaw = 0;
        microJitterPitch = 0;
        jitterFrameCount = 0;
    }

    /**
     * Called from tick event - but we use render event for smoother rotation
     */
    public void tick() {
        // Tick is now empty - all rotation is done in onRender for smoothness
    }

    /**
     * Called every render frame for smooth rotation.
     * This should be called from the world render event.
     */
    public void onRender() {
        if (!isActive) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        ClientPlayerEntity player = client.player;
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderTime) / 1000.0f, 0.05f);
        lastRenderTime = now;

        if (dt <= 0) return;

        // Handle target mode (for etherwarp)
        if (targetMode && targetPoint != null) {
            renderTargetMode(player, dt);
            return;
        }

        // Path following mode
        if (path == null || path.isEmpty() || currentNodeIndex >= path.size()) {
            return;
        }

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        // Get lookahead target with line of sight
        Vec3d target = getLookaheadWithLOS(px, py + 1.62, pz, client.world);
        if (target == null) return;

        // Calculate yaw to target
        double dx = target.x - px;
        double dz = target.z - pz;
        float pathTargetYaw = normalizeAngle((float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f);

        // Predict pitch based on upcoming terrain
        float pathTargetPitch = predictPitch(px, py, pz);

        // Speed adjustment for corners (use config values)
        float yawDiff = Math.abs(angleDiff(smoothYaw, pathTargetYaw));
        float speedMult = yawDiff > CORNER_THRESHOLD ? cornerBoost : 1.0f;

        // Smooth interpolation using exponential decay (use config speeds)
        float yawAlpha = 1.0f - (float) Math.exp(-yawSpeed * speedMult * dt);
        float pitchAlpha = 1.0f - (float) Math.exp(-pitchSpeed * dt);

        smoothYaw += angleDiff(smoothYaw, pathTargetYaw) * yawAlpha;
        smoothYaw = normalizeAngle(smoothYaw);
        smoothPitch += (pathTargetPitch - smoothPitch) * pitchAlpha;
        smoothPitch = MathHelper.clamp(smoothPitch, -50.0f, 60.0f);

        // Apply rotation
        player.setYaw(smoothYaw);
        player.setPitch(smoothPitch);
    }

    /**
     * Handle rotation for target mode (etherwarp, etc).
     * Uses human-like rotation with natural overshoot and correction.
     */
    private void renderTargetMode(ClientPlayerEntity player, float dt) {
        // Check for timeout
        long elapsed = System.currentTimeMillis() - targetStartTime;
        if (elapsed > TARGET_TIMEOUT_MS) {
            WieselClient.LOGGER.warn("[RotationManager] Target rotation timeout after {}ms, completing naturally", elapsed);
            fireTargetCallback();
            return;
        }

        // Recalculate target angles using appropriate eye height
        double eyeHeight = targetWhileSneaking ? SNEAKING_EYE_HEIGHT : STANDING_EYE_HEIGHT;
        Vec3d playerPos = player.getPos();
        Vec3d playerEyes = playerPos.add(0, eyeHeight, 0);

        double dx = targetPoint.x - playerEyes.x;
        double dy = targetPoint.y - playerEyes.y;
        double dz = targetPoint.z - playerEyes.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        targetPitch = (float) -(Math.atan2(dy, distXZ) * 180.0 / Math.PI);
        targetYaw = MathHelper.wrapDegrees(targetYaw);
        targetPitch = MathHelper.clamp(targetPitch, -90, 90);

        // Load config speed
        float configSpeed = WieselClient.getInstance().getConfigManager()
            .getFloat("etherwarp.rotationSpeed", DEFAULT_ETHERWARP_SPEED);

        // Determine actual target with overshoot behavior
        float effectiveTargetYaw = targetYaw;
        float effectiveTargetPitch = targetPitch;

        // Phase 1: Overshoot (if enabled and we haven't done it yet)
        if (!hasOvershot && (overshootYaw != 0 || overshootPitch != 0)) {
            effectiveTargetYaw = targetYaw + overshootYaw;
            effectiveTargetPitch = targetPitch + overshootPitch;
        }

        float yawDiff = angleDiff(smoothYaw, effectiveTargetYaw);
        float pitchDiff = effectiveTargetPitch - smoothPitch;
        float absYawDiff = Math.abs(yawDiff);
        float absPitchDiff = Math.abs(pitchDiff);

        // Check if overshoot phase is complete
        if (!hasOvershot && (overshootYaw != 0 || overshootPitch != 0)) {
            // When close to overshoot target, mark as complete
            if (absYawDiff < 0.5f && absPitchDiff < 0.4f) {
                hasOvershot = true;
                WieselClient.LOGGER.info("[RotationManager] Overshoot phase complete, correcting back to target");
            }
        }

        // Update micro jitter every few frames for human hand instability
        // Completely disable jitter when very close to target for final precision
        jitterFrameCount++;
        if (jitterFrameCount >= JITTER_UPDATE_FRAMES) {
            jitterFrameCount = 0;

            // Calculate distance to actual target (not overshoot)
            float distToTarget = Math.abs(angleDiff(smoothYaw, targetYaw)) + Math.abs(targetPitch - smoothPitch);

            // Disable jitter completely when very close for pixel-perfect aim
            if (distToTarget < 1.5f) {
                microJitterYaw = 0;
                microJitterPitch = 0;
            } else {
                // Reduce jitter when getting close
                float jitterMult = 1.0f;
                if (distToTarget < 5.0f) {
                    jitterMult = 0.5f; // Half jitter when approaching
                }

                // Random micro movement in both directions
                microJitterYaw = (float) ((Math.random() - 0.5) * 2.0 * MICRO_JITTER_AMOUNT * jitterMult);
                microJitterPitch = (float) ((Math.random() - 0.5) * 2.0 * MICRO_JITTER_AMOUNT * jitterMult);
            }
        }

        // Base speed from config with variance
        float baseYawSpeed = configSpeed * speedVariance;
        float basePitchSpeed = (configSpeed * 0.88f) * speedVariance; // Pitch slightly slower

        // Get hardcoded pattern multipliers based on distance
        float yawSpeedMult = getPatternSpeedMultiplier(speedPattern, absYawDiff, true);
        float pitchSpeedMult = getPatternSpeedMultiplier(speedPattern, absPitchDiff, false);

        float yawSpeed = baseYawSpeed * yawSpeedMult;
        float pitchSpeed = basePitchSpeed * pitchSpeedMult;

        float yawAlpha = 1.0f - (float) Math.exp(-yawSpeed * dt);
        float pitchAlpha = 1.0f - (float) Math.exp(-pitchSpeed * dt);

        // Apply rotation incrementally
        smoothYaw += yawDiff * yawAlpha;
        smoothYaw = normalizeAngle(smoothYaw);

        smoothPitch += pitchDiff * pitchAlpha;
        smoothPitch = MathHelper.clamp(smoothPitch, -90, 90);

        // Apply micro jitter for human-like hand instability
        float finalYaw = smoothYaw + microJitterYaw;
        float finalPitch = smoothPitch + microJitterPitch;

        finalYaw = normalizeAngle(finalYaw);
        finalPitch = MathHelper.clamp(finalPitch, -90, 90);

        // Apply to player with jitter
        player.setYaw(finalYaw);
        player.setPitch(finalPitch);

        // Check if we're close enough to the ACTUAL target (not overshoot)
        float finalYawDiff = Math.abs(angleDiff(smoothYaw, targetYaw));
        float finalPitchDiff = Math.abs(targetPitch - smoothPitch);

        // Completion check - must be close and overshoot phase complete (if enabled)
        boolean closeEnough = finalYawDiff < ETHERWARP_COMPLETION_THRESHOLD &&
                             finalPitchDiff < ETHERWARP_COMPLETION_THRESHOLD;
        boolean overshootDone = (overshootYaw == 0 && overshootPitch == 0) || hasOvershot;

        if (closeEnough && overshootDone) {
            // Force jitter to absolute zero before firing callback
            microJitterYaw = 0;
            microJitterPitch = 0;

            // Set rotation to exact target with zero jitter
            player.setYaw(targetYaw);
            player.setPitch(targetPitch);

            fireTargetCallback();
        }
    }

    /**
     * Fire the target reached callback (without any snapping).
     */
    private void fireTargetCallback() {
        if (onTargetReached != null) {
            Runnable callback = onTargetReached;
            onTargetReached = null;
            targetMode = false;
            targetWhileSneaking = false;
            isActive = false;
            hasOvershot = false;
            microJitterYaw = 0;
            microJitterPitch = 0;
            jitterFrameCount = 0;

            WieselClient.LOGGER.info("[RotationManager] Target reached naturally (yaw={}, pitch={}), firing callback",
                String.format("%.2f", smoothYaw), String.format("%.2f", smoothPitch));
            try {
                callback.run();
            } catch (Exception e) {
                WieselClient.LOGGER.error("[RotationManager] Callback error", e);
            }
        }
    }

    /**
     * Get lookahead target with line of sight checking and corner detection.
     */
    private Vec3d getLookaheadWithLOS(double px, double eyeY, double pz, World world) {
        if (path == null) return null;

        Vec3d bestTarget = null;
        double bestDist = 0;
        int bestNodeIndex = currentNodeIndex;

        int maxIdx = Math.min(currentNodeIndex + lookaheadNodes, path.size() - 1);

        // Find farthest visible node
        for (int i = currentNodeIndex; i <= maxIdx; i++) {
            PathNode node = path.get(i);
            double nx = node.x + 0.5;
            double ny = node.y + 1.0;
            double nz = node.z + 0.5;

            double dist = Math.sqrt(Math.pow(nx - px, 2) + Math.pow(nz - pz, 2));

            // Skip if too close (unless it's the last node)
            if (dist < lookaheadMinDist && i < maxIdx) {
                continue;
            }

            // Skip if too far
            if (dist > lookaheadMaxDist) {
                break;
            }

            // Check line of sight if enabled
            boolean visible = !enableLOS || hasLineOfSight(px, eyeY, pz, nx, ny, nz, world);

            if (visible) {
                // Check if this is a corner (sharp direction change)
                if (i > currentNodeIndex && i < path.size() - 1) {
                    if (isCorner(i)) {
                        // Corner detected - don't look past it, target the corner
                        bestTarget = new Vec3d(nx, ny, nz);
                        bestNodeIndex = i;
                        break;
                    }
                }

                bestTarget = new Vec3d(nx, ny, nz);
                bestDist = dist;
                bestNodeIndex = i;
            } else if (bestTarget != null) {
                // Lost LOS - stop here and use last good target
                break;
            }
        }

        // Fallback to current node if nothing found
        if (bestTarget == null && currentNodeIndex < path.size()) {
            PathNode node = path.get(currentNodeIndex);
            bestTarget = new Vec3d(
                node.x + 0.5,
                node.y + 1.0,
                node.z + 0.5
            );
        }

        return bestTarget;
    }

    /**
     * Check if a node is a corner (sharp direction change).
     * This prevents looking past corners into walls.
     */
    private boolean isCorner(int nodeIndex) {
        if (nodeIndex < 1 || nodeIndex >= path.size() - 1) return false;

        PathNode prev = path.get(nodeIndex - 1);
        PathNode curr = path.get(nodeIndex);
        PathNode next = path.get(nodeIndex + 1);

        // Calculate direction vectors
        double dx1 = curr.x - prev.x;
        double dz1 = curr.z - prev.z;
        double dx2 = next.x - curr.x;
        double dz2 = next.z - curr.z;

        // Normalize vectors
        double len1 = Math.sqrt(dx1 * dx1 + dz1 * dz1);
        double len2 = Math.sqrt(dx2 * dx2 + dz2 * dz2);

        if (len1 < 0.01 || len2 < 0.01) return false;

        dx1 /= len1;
        dz1 /= len1;
        dx2 /= len2;
        dz2 /= len2;

        // Calculate dot product (cos of angle between vectors)
        double dotProduct = dx1 * dx2 + dz1 * dz2;

        // If dot product < 0.5, angle > 60 degrees = sharp corner
        return dotProduct < 0.5;
    }

    /**
     * Check line of sight between two points.
     */
    private boolean hasLineOfSight(double x1, double y1, double z1, double x2, double y2, double z2, World world) {
        if (world == null) return true;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 1) return true;

        int steps = (int) Math.ceil(dist * 2);

        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            int cx = (int) Math.floor(x1 + dx * t);
            int cy = (int) Math.floor(y1 + dy * t);
            int cz = (int) Math.floor(z1 + dz * t);

            BlockPos pos = new BlockPos(cx, cy, cz);
            BlockState state = world.getBlockState(pos);

            if (!isTransparent(state)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if block is see-through for LOS.
     */
    private boolean isTransparent(BlockState state) {
        String name = state.getBlock().getTranslationKey().toLowerCase();
        return name.contains("air") ||
               name.contains("glass") ||
               name.contains("water") ||
               name.contains("lava") ||
               name.contains("leaves") ||
               name.contains("fence") ||
               name.contains("bars") ||
               name.contains("torch") ||
               name.contains("lantern") ||
               name.contains("flower") ||
               name.contains("grass") ||
               name.contains("fern") ||
               name.contains("vine") ||
               name.contains("sapling") ||
               name.contains("carpet") ||
               name.contains("sign") ||
               name.contains("banner") ||
               name.contains("pressure") ||
               name.contains("button") ||
               name.contains("lever") ||
               name.contains("rail");
    }

    /**
     * Predict pitch based on upcoming terrain changes.
     */
    private float predictPitch(double px, double py, double pz) {
        if (path == null || currentNodeIndex >= path.size()) {
            return BASE_PITCH;
        }

        int checkNodes = Math.min(PREDICTION_NODES, path.size() - currentNodeIndex - 1);
        if (checkNodes <= 0) return BASE_PITCH;

        // Get current Y level
        PathNode startNode = path.get(currentNodeIndex);
        double startY = startNode.y;

        // Track elevation changes
        double totalDy = 0;
        double maxUp = 0;
        double maxDown = 0;
        int upCount = 0;
        int downCount = 0;

        for (int i = 1; i <= checkNodes; i++) {
            int idx = currentNodeIndex + i;
            if (idx >= path.size()) break;

            PathNode node = path.get(idx);
            PathNode prevNode = path.get(idx - 1);
            double dy = node.y - prevNode.y;

            if (dy > 0.3) {
                // Going up
                upCount++;
                maxUp = Math.max(maxUp, dy);
                totalDy += dy;
            } else if (dy < -0.3) {
                // Going down
                downCount++;
                maxDown = Math.min(maxDown, dy);
                totalDy += dy;
            }
        }

        // Determine pitch based on prediction
        if (upCount >= 2 || maxUp >= 0.9) {
            // Significant climb ahead - look up
            float intensity = Math.min(upCount / 3.0f, 1.0f);
            return CLIMB_PITCH * intensity + BASE_PITCH * (1 - intensity);
        } else if (downCount >= 2 || maxDown <= -0.9) {
            // Significant descent ahead - look down
            float intensity = Math.min(downCount / 3.0f, 1.0f);
            return DESCEND_PITCH * intensity + BASE_PITCH * (1 - intensity);
        } else if (totalDy > 1) {
            // Gradual climb
            return CLIMB_PITCH * 0.5f + BASE_PITCH * 0.5f;
        } else if (totalDy < -1) {
            // Gradual descent
            return DESCEND_PITCH * 0.5f + BASE_PITCH * 0.5f;
        }

        return BASE_PITCH;
    }

    /**
     * Get speed multiplier based on hardcoded pattern and distance.
     * All patterns scale with the config base speed.
     */
    private float getPatternSpeedMultiplier(int pattern, float distance, boolean isYaw) {
        // Adjust thresholds for yaw vs pitch
        float farThreshold = isYaw ? 30.0f : 20.0f;
        float midThreshold = isYaw ? 15.0f : 10.0f;
        float nearThreshold = isYaw ? 5.0f : 4.0f;

        switch (pattern) {
            case 0: // SMOOTH - consistent speed with slight slowdown at end
                if (distance > farThreshold) return 1.15f;
                if (distance > midThreshold) return 1.1f;
                if (distance < nearThreshold) return 0.75f;
                return 1.0f;

            case 1: // QUICK_START - fast beginning, gradual slowdown
                if (distance > farThreshold) return 1.35f;
                if (distance > midThreshold) return 1.15f;
                if (distance > nearThreshold) return 0.9f;
                return 0.7f;

            case 2: // ACCELERATE - slow start, fast middle, slow end
                if (distance > farThreshold) return 0.9f;
                if (distance > midThreshold) return 1.3f;
                if (distance < nearThreshold) return 0.75f;
                return 1.05f;

            case 3: // STEADY - very consistent with tiny variations
                if (distance > farThreshold) return 1.05f;
                if (distance < nearThreshold) return 0.95f;
                return 1.0f;

            case 4: // BURST - quick movements with slight pauses (variable)
                if (distance > farThreshold) return 1.25f;
                if (distance > midThreshold) return 1.35f;
                if (distance > nearThreshold) return 1.1f;
                return 0.8f;

            default:
                return 1.0f;
        }
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

    // Getters
    public boolean isActive() {
        return isActive;
    }

    public float getTargetYaw() {
        return smoothYaw;
    }

    public float getTargetPitch() {
        return smoothPitch;
    }
}
