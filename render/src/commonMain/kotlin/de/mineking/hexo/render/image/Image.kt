package de.mineking.hexo.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.HighlightLine
import de.mineking.hexo.board.end
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@IgnorableReturnValue
fun <R : RenderingContext> Board.render(
    layoutRadius: Double,
    focusWinningRows: Boolean = true,
    theme: Theme = BasicTheme.Default,
    renderingContext: (BoundingBox) -> R,
): R {
    val size = RenderSize(layoutRadius)
    val visibleCoordinates = findVisibleCoordinates()
    val boundingBox = findBoundingBox(size, visibleCoordinates)
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

        for (position in visibleCoordinates) {
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

val BoundingBox.center get() = Point(
    (maxX + minX) / 2,
    (maxY + minY) / 2,
)

val BoundingBox.topLeft get() = Point(minX, minY)

data class BoardRenderLayout(
    val layoutRadius: Double,
    val boundingBox: BoundingBox,
) {
    fun toCoordinate(point: Point): CellCoordinate {
        val x = point.x + boundingBox.minX
        val y = point.y + boundingBox.minY
        val r = y / (layoutRadius * 1.5)
        val q = x / (layoutRadius * RenderSize.sqrt3) - r / 2.0

        return roundAxial(q, r)
    }

    private fun roundAxial(q: Double, r: Double): CellCoordinate {
        var roundedQ = q.roundToInt()
        val roundedS = (-q - r).roundToInt()
        var roundedR = r.roundToInt()

        val qDiff = abs(roundedQ - q)
        val sDiff = abs(roundedS - (-q - r))
        val rDiff = abs(roundedR - r)

        when {
            qDiff > sDiff && qDiff > rDiff -> roundedQ = -roundedS - roundedR
            rDiff > sDiff -> roundedR = -roundedQ - roundedS
        }

        return CellCoordinate(roundedQ, roundedR)
    }
}

fun Board.createRenderLayout(layoutRadius: Double): BoardRenderLayout {
    val size = RenderSize(layoutRadius)
    return BoardRenderLayout(layoutRadius, findBoundingBox(size, findVisibleCoordinates()))
}

private const val VISIBLE_DISTANCE = 8

private fun Board.findVisibleCoordinates(): Set<CellCoordinate> {
    val occupied = cells
        .filterValues { it.owner != null }
        .keys
        .ifEmpty { setOf(CellCoordinate.Zero) }

    return buildSet {
        for (origin in occupied) {
            for (dq in -VISIBLE_DISTANCE + 1 until VISIBLE_DISTANCE) {
                for (dr in -VISIBLE_DISTANCE + 1 until VISIBLE_DISTANCE) {
                    val coordinate = CellCoordinate(origin.q + dq, origin.r + dr)
                    if (origin.distanceTo(coordinate) < VISIBLE_DISTANCE) {
                        add(coordinate)
                    }
                }
            }
        }
    }
}

private fun Board.findBoundingBox(size: RenderSize, visibleCoordinates: Set<CellCoordinate>): BoundingBox {
    var minX = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY

    val endPoints = highlightedLines.flatMap { listOf(it.start, it.end) }
    val positions = visibleCoordinates + cells.keys + endPoints
    for (position in positions.ifEmpty { listOf(CellCoordinate.Zero) }) {
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

private fun CellCoordinate.distanceTo(other: CellCoordinate): Int {
    val ds = (q + r) - (other.q + other.r)
    return maxOf(abs(q - other.q), abs(r - other.r), abs(ds))
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
