package de.mineking.hexo.render.image

import org.w3c.dom.CanvasRenderingContext2D

class CanvasRenderingContext(val canvas: CanvasRenderingContext2D) : RenderingContext {
    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        TODO("Not yet implemented")
    }

    override fun drawPolygon(shape: Polygon, color: Color?, outline: Stroke?) {
        TODO("Not yet implemented")
    }

    override fun drawString(point: Point, text: String, fontSize: Float, color: Color?) {
        TODO("Not yet implemented")
    }

    override fun close() {
        // Nothing to do
    }
}
