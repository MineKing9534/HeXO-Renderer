package de.mineking.hexo.render.image

data class Point(val x: Double, val y: Double)
data class Polygon(val points: List<Point>)

data class Stroke(val color: Color, val width: Float)

interface RenderingContext : AutoCloseable {
    fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke? = null)
    fun drawPolygon(shape: Polygon, color: Color?, outline: Stroke? = null)
    fun drawString(point: Point, text: String, fontSize: Float, color: Color?)
}
