package com.wiesel.client.rendering

import com.wiesel.client.rendering.layers.ChromaRenderLayer
import net.minecraft.Util
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.RenderType.CompositeRenderType
import net.minecraft.client.renderer.RenderType.CompositeState
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.TriState
import java.util.OptionalDouble
import java.util.concurrent.ConcurrentHashMap

object WieselRenderLayers {

    private val linesCache = ConcurrentHashMap<Int, CompositeRenderType>()
    private val linesThroughWallsCache = ConcurrentHashMap<Int, CompositeRenderType>()

    private val FILLED: CompositeRenderType = RenderType.create(
        "wiesel_filled",
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        WieselRenderPipeline.FILLED(),
        CompositeState.builder().setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING).createCompositeState(false),
    )

    private val FILLED_XRAY: CompositeRenderType = RenderType.create(
        "wiesel_filled_xray",
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        WieselRenderPipeline.FILLED_XRAY(),
        CompositeState.builder().createCompositeState(false),
    )

    private val TRIANGLES: CompositeRenderType = RenderType.create(
        "wiesel_triangles",
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        WieselRenderPipeline.TRIANGLES(),
        CompositeState.builder().setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING).createCompositeState(false),
    )

    private val TRIANGLES_XRAY: CompositeRenderType = RenderType.create(
        "wiesel_triangles_xray",
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        WieselRenderPipeline.TRIANGLES_XRAY(),
        CompositeState.builder().createCompositeState(false),
    )

    private val TRIANGLE_FAN: CompositeRenderType = RenderType.create(
        "wiesel_triangle_fan",
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        WieselRenderPipeline.TRIANGLE_FAN(),
        CompositeState.builder().setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING).createCompositeState(false),
    )

    private val TRIANGLE_FAN_XRAY: CompositeRenderType = RenderType.create(
        "wiesel_triangle_fan_xray",
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        WieselRenderPipeline.TRIANGLE_FAN_XRAY(),
        CompositeState.builder().createCompositeState(false),
    )

    private val QUADS: CompositeRenderType = RenderType.create(
        "wiesel_quads",
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        WieselRenderPipeline.QUADS(),
        CompositeState.builder().setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING).createCompositeState(false),
    )

    private val QUADS_XRAY: CompositeRenderType = RenderType.create(
        "wiesel_quads_xray",
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        WieselRenderPipeline.QUADS_XRAY(),
        CompositeState.builder().createCompositeState(false),
    )

    private val CHROMA_STANDARD: CompositeRenderType = ChromaRenderLayer(
        "wiesel_standard_chroma",
        RenderType.SMALL_BUFFER_SIZE,
        false,
        false,
        WieselRenderPipeline.CHROMA_STANDARD(),
        CompositeState.builder().createCompositeState(false),
    )

    private val CHROMA_TEXTURED: java.util.function.Function<ResourceLocation, RenderType> = Util.memoize { texture ->
        ChromaRenderLayer(
            "wiesel_text_chroma",
            RenderType.SMALL_BUFFER_SIZE,
            false,
            false,
            WieselRenderPipeline.CHROMA_TEXT(),
            CompositeState.builder()
                //#if MC < 1.21.6
                .setTextureState(RenderStateShard.TextureStateShard(texture, TriState.FALSE, false))
                //#else
                //$$ .setTextureState(RenderStateShard.TextureStateShard(texture, false))
                //#endif
                .createCompositeState(false),
        )
    }

    private fun createLineRenderLayer(lineWidth: Double, throughWalls: Boolean): CompositeRenderType {
        val pipeLine = if (throughWalls) WieselRenderPipeline.LINES_XRAY() else WieselRenderPipeline.LINES()
        return RenderType.create(
            "wiesel_lines_${lineWidth}${if (throughWalls) "_xray" else ""}",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            pipeLine,
            CompositeState.builder()
                .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(lineWidth)))
                .setLayeringState(if (throughWalls) RenderStateShard.NO_LAYERING else RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false),
        )
    }

    fun getFilled(throughWalls: Boolean): CompositeRenderType {
        return if (throughWalls) FILLED_XRAY else FILLED
    }

    fun getTriangles(throughWalls: Boolean): CompositeRenderType {
        return if (throughWalls) TRIANGLES_XRAY else TRIANGLES
    }

    fun getTriangleFan(throughWalls: Boolean): CompositeRenderType {
        return if (throughWalls) TRIANGLE_FAN_XRAY else TRIANGLE_FAN
    }

    fun getQuads(throughWalls: Boolean): CompositeRenderType {
        return if (throughWalls) QUADS_XRAY else QUADS
    }

    fun getLines(lineWidth: Double, throughWalls: Boolean): CompositeRenderType {
        val cache = if (throughWalls) linesThroughWallsCache else linesCache
        return cache.computeIfAbsent(lineWidth.hashCode()) {
            createLineRenderLayer(lineWidth, throughWalls)
        }
    }

    fun getChromaTexturedWithIdentifier(identifier: ResourceLocation) = CHROMA_TEXTURED.apply(identifier)

    //#if MC < 1.21.6
    fun getChromaStandard() = CHROMA_STANDARD
    fun getChromaTextured() = WieselRenderLayers::getChromaTexturedWithIdentifier
    //#else
    //$$ fun getChromaStandard(): com.mojang.blaze3d.pipeline.RenderPipeline = WieselRenderPipeline.CHROMA_STANDARD()
    //$$ fun getChromaTextured(): com.mojang.blaze3d.pipeline.RenderPipeline = WieselRenderPipeline.CHROMA_TEXT()
    //#endif

}
