package com.wiesel.client.config;

public class WieselConfig {
    public PathfinderSettings pathfinder = new PathfinderSettings();
    public RenderSettings render = new RenderSettings();
    public RotationSettings rotation = new RotationSettings();
    public EtherwarpSettings etherwarp = new EtherwarpSettings();

    public static class PathfinderSettings {
        public boolean autoStart = true;
        public String defaultMap = "hub";
        public int keepaliveInterval = 60000; // 60 seconds
    }

    public static class RenderSettings {
        public boolean enabled = true;
        public int pathLineColor = 0x00AA00; // Green
        public int nodeHighlightColor = 0x00FF00; // Bright green
        public int nodeHighlightInterval = 15; // Highlight every 15 blocks
        public float pathLineWidth = 2.0f;
        public float nodeAlpha = 0.5f;
    }

    public static class RotationSettings {
        public float yawSpeed = 7.0f;
        public float pitchSpeed = 4.5f;
        public float lookahead = 8.0f;
        public float lookaheadMinDist = 4.0f;
        public float lookaheadMaxDist = 15.0f;
        public boolean enableLOS = true;
        public float cornerBoost = 1.5f;
    }

    public static class EtherwarpSettings {
        public float rotationSpeed = 8.5f;
        public float overshootAmount = 1.5f;
        public boolean enableOvershoot = true;
        public float speedVariation = 0.3f;
    }
}
