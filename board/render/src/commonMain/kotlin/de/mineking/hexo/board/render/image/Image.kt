package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.MutableCell
import de.mineking.hexo.board.end
import kotlin.math.cos
import kotlin.math.sin

@IgnorableReturnValue
fun RenderingContext.drawBoard(
    layout: BoardRenderLayout,
    theme: Theme = BasicTheme.Default,
    middleLayer: InternalBoardRenderer.() -> Unit = {},
) {
    val renderer = InternalBoardRenderer(this, layout, theme)

    for (position in layout.coordinates) {
        val cell = layout.board.cells[position] ?: Cell()
        renderer.drawCell(position, cell)
    }

    renderer.middleLayer()

    layout.board.lineHighlights.forEach {
        renderer.drawLine(it)
    }
}

class InternalBoardRenderer(
    val renderingContext: RenderingContext,
    val layout: BoardRenderLayout,
    val theme: Theme,
) {
    companion object {
        private val FOCUS = MutableCell(focused = true)
    }

    val hexSize = layout.size.layoutRadius * (1 - theme.gap / 64)
    val borderThickness = theme.borderThickness.relativeWidth()
    val lineThickness = theme.lineThickness.relativeWidth()

    fun drawLine(line: LineHighlight) {
        val (backgroundColor, borderColor) = theme.run { line.color() }
        if (backgroundColor.isTransparent() && borderColor.isTransparent()) return

        renderingContext.drawLine(
            from = line.start.toPixel(),
            to = line.end.toPixel(),
            stroke = Stroke(backgroundColor, lineThickness),
            outline = Stroke(borderColor, lineThickness / 3),
        )
    }

    fun drawCell(position: CellCoordinate, cell: Cell) {
        val pixel = position.toPixel()
        val hex = pixel.createHex(hexSize)

        if (hex.points.none { it in layout.boundingBox }) return

        val (backgroundColor, borderColor) = theme.run { cell.backgroundColor() }
        renderingContext.drawPolygon(hex, backgroundColor, Stroke(borderColor, borderThickness))

        val (highlightColor, highlightBorderColor) = theme.run { cell.highlightColor() }
        if (!highlightColor.isTransparent() || !highlightBorderColor.isTransparent()) {
            renderingContext.drawPolygon(hex, highlightColor, Stroke(highlightBorderColor, borderThickness * 3))
        }

        if (cell.highlight != null && cell.owner != null) {
            val point = position.toPixel()
            val (_, color) = theme.run { FOCUS.highlightColor() }
            renderingContext.drawLine(point, point, Stroke(color, borderThickness * 8))
        }

        drawCellLabel(pixel, cell)
    }

    private fun drawCellLabel(pixel: Point, cell: Cell) {
        val (label, color, font) = theme.run { cell.label() }
            ?: return theme.run { cell.drawDecoration(pixel, this@InternalBoardRenderer) }

        if (color.isTransparent()) return

        renderingContext.drawString(
            point = pixel,
            text = label,
            fontSize = hexSize.toFloat() * font.fontSize,
            font = font.type,
            color = color,
        )
    }

    fun CellCoordinate.toPixel() = layout.size.run { toPixel() }

    private fun Double.relativeWidth() = (layout.size.layoutRadius * this / 64).toFloat()
}

private const val DEGREES_TO_RADIANS = 0.017453292519943295
fun Point.createHex(radius: Double) = Polygon((0 until 6).map {
    val angle = DEGREES_TO_RADIANS * (60.0 * it - 30)

    val x = x + radius * cos(angle)
    val y = y + radius * sin(angle)

    Point(x, y)
})
