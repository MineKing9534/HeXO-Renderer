package de.mineking.hexo.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.HighlightLine
import de.mineking.hexo.board.end
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

fun <R : RenderingContext> Board.render(
    layoutRadius: Double,
    focusWinningRows: Boolean = true,
    theme: Theme = BasicTheme.Default,
    renderingContext: (BoundingBox) -> R,
): R {
    val size = RenderSize(layoutRadius)
    val boundingBox = findBoundingBox(size)
    val renderingContext = renderingContext(boundingBox)
    val renderer = InternalBoardRenderer(
        renderingContext,
        boundingBox,
        size,
        theme,
    )

    renderer.use {
        val winningCells =
            if (focusWinningRows) {
                findWinningRows().flatten().map { it.first }.toSet()
            } else {
                emptyList()
            }

        for (position in boundingBox.findVisibleCoordinates(size)) {
            val cell = cells[position]?.let { it.copy(focussed = it.focussed || position in winningCells) } ?: Cell()
            renderer.drawCell(position, cell)
        }

        highlightedLines.forEach {
            renderer.drawLine(it)
        }
    }

    return renderingContext
}

data class BoundingBox(
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
)

private fun BoundingBox.findVisibleCoordinates(size: RenderSize) = sequence {
    val horizontalStep = size.layoutRadius * RenderSize.sqrt3
    val verticalStep = size.layoutRadius * 1.5

    val minR = floor(minY / verticalStep).toInt() - 1
    val maxR = ceil(maxY / verticalStep).toInt() + 1

    for (r in minR..maxR) {
        val rOffset = r / 2.0
        val minQ = floor(minX / horizontalStep - rOffset).toInt() - 1
        val maxQ = ceil(maxX / horizontalStep - rOffset).toInt() + 1

        for (q in minQ..maxQ) {
            yield(CellCoordinate(q, r))
        }
    }
}

private fun Board.findBoundingBox(size: RenderSize): BoundingBox {
    var minX = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY

    val endPoints = highlightedLines.flatMap { listOf(it.start, it.end) }
    for (position in cells.keys + endPoints) {
        val center = size.run { position.toPixel() }
        val hex = center.createHex(size.layoutRadius)

        hex.points.forEach { (x, y) ->
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }
    }

    return BoundingBox(
        minX = minX,
        maxX = maxX,
        minY = minY,
        maxY = maxY,
    )
}

private class RenderSize(val layoutRadius: Double) {
    companion object {
        val sqrt3 = sqrt(3.0)
    }

    fun CellCoordinate.toPixel(): Point {
        val x = layoutRadius * (sqrt3 * q + sqrt3 / 2 * r)
        val y = layoutRadius * (3.0 / 2 * r)
        return Point(x, y)
    }
}

private class InternalBoardRenderer(
    private val renderingContext: RenderingContext,
    private val boundingBox: BoundingBox,
    private val size: RenderSize,
    private val theme: Theme,
) : AutoCloseable {
    private val hexSize = size.layoutRadius * (1 - theme.gap / 64)
    private val borderThickness = (size.layoutRadius * theme.borderThickness / 64).toFloat()

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
        val hex = position.toPixel().createHex(hexSize)

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

    override fun close() {
        renderingContext.close()
    }

    private fun CellCoordinate.toPixel() = size.run {
        toPixel().let { (x, y) -> Point(x - boundingBox.minX, y - boundingBox.minY) }
    }
}

private const val DEGREES_TO_RADIANS = 0.017453292519943295
private fun Point.createHex(radius: Double) = Polygon((0 until 6).map {
    val angle = DEGREES_TO_RADIANS * (60.0 * it - 30)

    val x = x + radius * cos(angle)
    val y = y + radius * sin(angle)

    Point(x, y)
})
