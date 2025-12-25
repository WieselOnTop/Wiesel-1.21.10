package com.wiesel.client.config;

public class WieselConfig {
    public PathfinderSettings pathfinder = new PathfinderSettings();
    public RenderSettings render = new RenderSettings();

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
}
