package de.mineking.hexo.render.image

data class Point(val x: Double, val y: Double) {
    companion object {
        val Zero = Point(0.0, 0.0)
    }
}

data class Polygon(val points: List<Point>)

data class Stroke(val color: Color, val width: Float)

interface RenderingContext {
    fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke? = null)
    fun drawPolygon(shape: Polygon, color: Color?, outline: Stroke? = null)
    fun drawString(point: Point, text: String, fontSize: Float, color: Color?)
}

operator fun Point.plus(point: Point) = Point(x + point.x, y + point.y)
operator fun Point.minus(point: Point) = Point(x - point.x, y - point.y)
operator fun Point.times(value: Double) = Point(x * value, y * value)
operator fun Point.div(value: Double) = Point(x / value, y / value)
