package com.wiesel.client.mixin;

import com.wiesel.client.pathfinder.PathWalker;
import com.wiesel.client.rendering.PathRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera,
                         GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager,
                         Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        MatrixStack matrices = new MatrixStack();
        PathRenderer.render(matrices, tickCounter.getTickDelta(false));

        // Call RotationManager for smooth human-like rotations
        PathWalker.getRotationManager().onRender();
    }
}
