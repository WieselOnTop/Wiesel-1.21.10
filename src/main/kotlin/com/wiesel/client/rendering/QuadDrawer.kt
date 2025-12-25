package com.wiesel.client.rendering

import com.wiesel.client.events.minecraft.WieselRenderWorldEvent
import com.wiesel.client.utils.LorenzVec
import java.awt.Color

class QuadDrawer @PublishedApi internal constructor(val event: WieselRenderWorldEvent) {

    inline fun draw(
        middlePoint: LorenzVec,
        sidePoint1: LorenzVec,
        sidePoint2: LorenzVec,
        c: Color,
    ) {
        val layer = WieselRenderLayers.getQuads(false)
        val buf = event.vertexConsumers.getBuffer(layer)
        event.matrices.pushPose()

        val viewerPos = WorldRenderUtils.getViewerPos()
        val newMidPoint = middlePoint - viewerPos
        val newSidePoint1 = sidePoint1 - viewerPos
        val newSidePoint2 = sidePoint2 - viewerPos
        val lastPoint = sidePoint1 + sidePoint2 - middlePoint
        val newLastPoint = lastPoint - viewerPos

        buf.addVertex(newSidePoint1.x.toFloat(), newSidePoint1.y.toFloat(), newSidePoint1.z.toFloat())
            .setColor(c.red, c.green, c.blue, c.alpha)
        buf.addVertex(newMidPoint.x.toFloat(), newMidPoint.y.toFloat(), newMidPoint.z.toFloat())
            .setColor(c.red, c.green, c.blue, c.alpha)
        buf.addVertex(newSidePoint2.x.toFloat(), newSidePoint2.y.toFloat(), newSidePoint2.z.toFloat())
            .setColor(c.red, c.green, c.blue, c.alpha)
        buf.addVertex(newLastPoint.x.toFloat(), newLastPoint.y.toFloat(), newLastPoint.z.toFloat())
            .setColor(c.red, c.green, c.blue, c.alpha)

        event.matrices.popPose()
    }

    companion object {
        inline fun draw3D(
            event: WieselRenderWorldEvent,
            crossinline quads: QuadDrawer.() -> Unit,
        ) {
            quads.invoke(QuadDrawer(event))
        }
    }
}
