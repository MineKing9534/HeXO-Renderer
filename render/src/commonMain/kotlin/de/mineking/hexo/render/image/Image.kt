package de.mineking.hexo.render.image

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.HighlightLine
import de.mineking.hexo.board.end
import kotlin.math.cos
import kotlin.math.sin

@IgnorableReturnValue
fun RenderingContext.drawBoard(
    layout: BoardRenderLayout,
    focusWinningRows: Boolean = true,
    theme: Theme = BasicTheme.Default,
    middleLayer: InternalBoardRenderer.() -> Unit = {},
) {
    val renderer = InternalBoardRenderer(this, layout, theme)

    val winningCells =
        if (focusWinningRows) {
            layout.board.findWinningRows().flatten().map { it.first }.toSet()
        } else {
            emptyList()
        }

    for (position in layout.coordinates) {
        val cell = layout.board.cells[position]?.let { it.copy(focussed = it.focussed || position in winningCells) } ?: Cell()
        renderer.drawCell(position, cell)
    }

    renderer.middleLayer()

    layout.board.highlightedLines.forEach {
        renderer.drawLine(it)
    }
}

class InternalBoardRenderer(
    val renderingContext: RenderingContext,
    val layout: BoardRenderLayout,
    val theme: Theme,
) {
    private val hexSize = layout.size.layoutRadius * (1 - theme.gap / 64)
    private val borderThickness = (layout.size.layoutRadius * theme.borderThickness / 64).toFloat()

    fun drawLine(line: HighlightLine) {
        val (backgroundColor, borderColor) = theme.run { line.color() }
        renderingContext.drawLine(
            from = line.start.toPixel(),
            to = line.end.toPixel(),
            stroke = Stroke(backgroundColor, borderThickness * 6),
            outline = Stroke(borderColor, borderThickness * 2),
        )
    }

    fun drawCell(position: CellCoordinate, cell: Cell) {
        val hex = position.createHex()

        renderingContext.drawPolygon(hex, theme.run { cell.backgroundColor() })

        fun drawHighlight(color: Theme.ElementColors) {
            renderingContext.drawPolygon(
                shape = hex,
                color = color.backgroundColor,
                outline = Stroke(color.borderColor, borderThickness * 3),
            )
        }

        when {
            cell.highlighted -> drawHighlight(theme.highlightColor)
            cell.focussed -> drawHighlight(theme.focusColor)
            else -> {
                renderingContext.drawPolygon(
                    shape = hex,
                    color = null,
                    outline = Stroke(theme.cellBorderColor, borderThickness),
                )
            }
        }

        drawCellLabel(position, cell)
    }

    fun drawCellLabel(position: CellCoordinate, cell: Cell) {
        val text = cell.label.takeIf { it.isNotBlank() }
            ?: cell.turn?.toString()
            ?: return

        renderingContext.drawString(
            point = position.toPixel(),
            text = text,
            fontSize = hexSize.toFloat() * 0.7f,
            color = theme.run { cell.labelColor() },
        )
    }

    fun CellCoordinate.toPixel() = layout.size.run {
        toPixel().let { (x, y) -> Point(x - layout.boundingBox.minX, y - layout.boundingBox.minY) }
    }

    fun CellCoordinate.createHex() = toPixel().createHex(hexSize)
}

private const val DEGREES_TO_RADIANS = 0.017453292519943295
internal fun Point.createHex(radius: Double) = Polygon((0 until 6).map {
    val angle = DEGREES_TO_RADIANS * (60.0 * it - 30)

    val x = x + radius * cos(angle)
    val y = y + radius * sin(angle)

    Point(x, y)
})
