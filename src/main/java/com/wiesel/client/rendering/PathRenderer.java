package com.wiesel.client.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wiesel.client.config.ConfigManager;
import com.wiesel.client.pathfinder.PathNode;
import com.wiesel.client.pathfinder.PathfindResponse;
import com.wiesel.client.pathfinder.PathfinderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;

public class PathRenderer {

    public static void render(MatrixStack matrices, float tickDelta) {
        PathfindResponse path = PathfinderManager.getLastPath();
        if (path == null || path.path == null || path.path.isEmpty()) {
            return;
        }

        if (!ConfigManager.getConfig().render.enabled) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Vec3d camera = mc.gameRenderer.getCamera().getPos();

        matrices.push();

        // Translate to camera position
        matrices.translate(-camera.x, -camera.y, -camera.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        // Draw path lines
        drawPathLines(buffer, matrices, path.path);

        // Draw node highlights
        drawNodeHighlights(buffer, matrices, path.path);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private static void drawPathLines(BufferBuilder buffer, MatrixStack matrices, List<PathNode> path) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Color color = new Color(ConfigManager.getConfig().render.pathLineColor);
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = 0.8f;

        for (int i = 0; i < path.size() - 1; i++) {
            PathNode current = path.get(i);
            PathNode next = path.get(i + 1);

            // Draw line from current to next
            buffer.vertex(matrix, current.x + 0.5f, current.y + 0.5f, current.z + 0.5f).color(r, g, b, a);
            buffer.vertex(matrix, next.x + 0.5f, next.y + 0.5f, next.z + 0.5f).color(r, g, b, a);
        }
    }

    private static void drawNodeHighlights(BufferBuilder buffer, MatrixStack matrices, List<PathNode> path) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int interval = ConfigManager.getConfig().render.nodeHighlightInterval;
        Color color = new Color(ConfigManager.getConfig().render.nodeHighlightColor);
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = ConfigManager.getConfig().render.nodeAlpha;

        for (int i = 0; i < path.size(); i += interval) {
            PathNode node = path.get(i);
            drawFilledBox(buffer, matrix, node.x, node.y, node.z, r, g, b, a);
        }

        // Always highlight the last node
        if (!path.isEmpty()) {
            PathNode lastNode = path.get(path.size() - 1);
            drawFilledBox(buffer, matrix, lastNode.x, lastNode.y, lastNode.z, r, g, b, a);
        }
    }

    private static void drawFilledBox(BufferBuilder buffer, Matrix4f matrix, int x, int y, int z, float r, float g, float b, float a) {
        // Draw a filled 1x1x1 box at the node position (ground level)
        float x1 = x;
        float y1 = y;
        float z1 = z;
        float x2 = x + 1;
        float y2 = y + 1;
        float z2 = z + 1;

        // Bottom face
        drawQuad(buffer, matrix, x1, y1, z1, x2, y1, z2, r, g, b, a);
        // Top face
        drawQuad(buffer, matrix, x1, y2, z1, x2, y2, z2, r, g, b, a);
        // North face
        drawQuad(buffer, matrix, x1, y1, z1, x2, y2, z1, r, g, b, a);
        // South face
        drawQuad(buffer, matrix, x1, y1, z2, x2, y2, z2, r, g, b, a);
        // West face
        drawQuad(buffer, matrix, x1, y1, z1, x1, y2, z2, r, g, b, a);
        // East face
        drawQuad(buffer, matrix, x2, y1, z1, x2, y2, z2, r, g, b, a);
    }

    private static void drawQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        // Draw 2 lines forming an X across the quad for visibility
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);

        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a);
    }
}
