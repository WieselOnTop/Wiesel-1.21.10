package com.wiesel.client.rendering

import com.wiesel.client.api.event.HandleEvent
import com.wiesel.client.events.minecraft.WieselRenderWorldEvent
import com.wiesel.client.wieselmodule.WieselModule
import com.wiesel.client.utils.LorenzVec
import com.wiesel.client.rendering.WorldRenderUtils.drawFilledBoundingBox
import com.wiesel.client.rendering.WorldRenderUtils.drawPyramid
import com.wiesel.client.rendering.WorldRenderUtils.drawString
import net.minecraft.world.phys.AABB
import java.awt.Color

@WieselModule
object DeferredDrawer {

    private val boxesNoDepth = mutableListOf<DeferredBox>()
    private val boxesDepth = mutableListOf<DeferredBox>()
    private val pyramidsNoDepth = mutableListOf<DeferredPyramid>()
    private val pyramidsDepth = mutableListOf<DeferredPyramid>()
    private val stringsNoDepth = mutableListOf<DeferredString>()
    private val stringsDepth = mutableListOf<DeferredString>()

    @HandleEvent(priority = 999)
    fun onRenderWorld(event: WieselRenderWorldEvent) {
        event.isCurrentlyDeferring = false
        boxesNoDepth.forEach { box ->
            event.drawFilledBoundingBox(
                box.aabb,
                box.color,
                box.alphaMultiplier,
                renderRelativeToCamera = true,
                seeThroughBlocks = true,
            )
        }
        boxesNoDepth.clear()
        boxesDepth.forEach { box ->
            event.drawFilledBoundingBox(
                box.aabb,
                box.color,
                box.alphaMultiplier,
                renderRelativeToCamera = true,
                seeThroughBlocks = false,
            )
        }
        boxesDepth.clear()
        pyramidsNoDepth.forEach { pyramid ->
            event.drawPyramid(
                pyramid.topPoint,
                pyramid.baseCenterPoint,
                pyramid.baseEdgePoint,
                pyramid.color,
                depth = false,
            )
        }
        pyramidsNoDepth.clear()
        pyramidsDepth.forEach { pyramid ->
            event.drawPyramid(
                pyramid.topPoint,
                pyramid.baseCenterPoint,
                pyramid.baseEdgePoint,
                pyramid.color,
                depth = true,
            )
        }
        pyramidsDepth.clear()
        stringsNoDepth.forEach { string ->
            event.drawString(
                string.location,
                string.text,
                seeThroughBlocks = true,
                string.color,
                string.scale,
                string.shadow,
                string.yOffset,
                string.backgroundColor,
            )
        }
        stringsNoDepth.clear()
        stringsDepth.forEach { string ->
            event.drawString(
                string.location,
                string.text,
                seeThroughBlocks = false,
                string.color,
                string.scale,
                string.shadow,
                string.yOffset,
                string.backgroundColor,
            )
        }
        stringsDepth.clear()
    }

    fun deferBox(
        aabb: AABB,
        color: Color,
        alphaMultiplier: Float,
        depth: Boolean = true,
    ) {
        val deferredBox = DeferredBox(aabb, color, alphaMultiplier)
        if (depth) {
            boxesDepth.add(deferredBox)
        } else {
            boxesNoDepth.add(deferredBox)
        }
    }

    fun deferPyramid(
        topPoint: LorenzVec,
        baseCenterPoint: LorenzVec,
        baseEdgePoint: LorenzVec,
        color: Color,
        depth: Boolean = true,
    ) {
        val deferredPyramid = DeferredPyramid(topPoint, baseCenterPoint, baseEdgePoint, color)
        if (depth) {
            pyramidsDepth.add(deferredPyramid)
        } else {
            pyramidsNoDepth.add(deferredPyramid)
        }
    }

    fun deferString(
        location: LorenzVec,
        text: String,
        color: Color?,
        scale: Double,
        shadow: Boolean,
        yOffset: Float,
        backgroundColor: Int,
        depth: Boolean,
    ) {
        val deferredString = DeferredString(location, text, color, scale, shadow, yOffset, backgroundColor)
        if (depth) {
            stringsDepth.add(deferredString)
        } else {
            stringsNoDepth.add(deferredString)
        }
    }

    data class DeferredBox(
        val aabb: AABB,
        val color: Color,
        val alphaMultiplier: Float,
    )

    data class DeferredPyramid(
        val topPoint: LorenzVec,
        val baseCenterPoint: LorenzVec,
        val baseEdgePoint: LorenzVec,
        val color: Color,
    )

    data class DeferredString(
        val location: LorenzVec,
        val text: String,
        val color: Color?,
        val scale: Double,
        val shadow: Boolean,
        val yOffset: Float,
        val backgroundColor: Int,
    )

}
