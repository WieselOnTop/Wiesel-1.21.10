package com.wiesel.client.mixin;

import com.wiesel.client.rendering.PathRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
                         net.minecraft.client.render.Camera camera, net.minecraft.client.render.GameRenderer gameRenderer,
                         net.minecraft.client.render.LightmapTextureManager lightmapTextureManager,
                         org.joml.Matrix4f projectionMatrix, CallbackInfo ci) {
        PathRenderer.render(matrices, tickDelta);
    }
}
