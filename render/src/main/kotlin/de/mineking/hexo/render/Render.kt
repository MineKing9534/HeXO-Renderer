@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.render

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Cell
import de.mineking.hexo.core.CellCoordinate
import de.mineking.hexo.core.Player
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class ColorScheme(
    val background: Color,
    val cellBorder: Color,
    val highlightedCellBorder: Color,
    val focussedCellBorder: Color,
    val emptyCell: Color,
    val playerX: Color,
    val playerO: Color,
) {
    companion object {
        val Default = ColorScheme(
            background = Color(0x0f172a),
            cellBorder = Color(0x202a3d),
            highlightedCellBorder = Color(0xec6fb1),
            focussedCellBorder = Color.WHITE,
            emptyCell = Color(0, true),
            playerX = Color(0xfbc139),
            playerO = Color(0x38bdf8),
        )
    }
}

fun Board.renderAsImage(
    layoutRadius: Double,
    gap: Double,
    borderThickness: Float,
    padding: Int,
    focusWinningRows: Boolean = true,
    colorScheme: ColorScheme = ColorScheme.Default,
): BufferedImage {
    require(cells.isNotEmpty())

    val size = RenderSize(layoutRadius, gap)
    val boundingBox = cells.keys.findBoundingBox(size)
    val renderer = BoardRenderer(
        boundingBox,
        size,
        colorScheme,
        borderThickness,
        padding,
    )

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

    return renderer.image
}

private data class BoundingBox(
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
)

private fun BoundingBox.findVisibleCoordinates(size: RenderSize) = sequence {
    val horizontalStep = size.layoutRadius * RenderSize.sqrt3
    val verticalStep = size.layoutRadius * 1.5
    val margin = size.hexSize

    val minR = floor((minY - margin) / verticalStep).toInt() - 1
    val maxR = ceil((maxY + margin) / verticalStep).toInt() + 1

    for (r in minR..maxR) {
        val rOffset = r / 2.0
        val minQ = floor((minX - margin) / horizontalStep - rOffset).toInt() - 1
        val maxQ = ceil((maxX + margin) / horizontalStep - rOffset).toInt() + 1

        for (q in minQ..maxQ) {
            yield(CellCoordinate(q, r))
        }
    }
}

private fun Collection<CellCoordinate>.findBoundingBox(size: RenderSize): BoundingBox {
    var minX = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY

    for (position in this) {
        val (cx, cy) = size.run { position.toPixel() }

        for (i in 0 until 6) {
            val angle = Math.toRadians(60.0 * i - 30)
            val x = cx + size.hexSize * cos(angle)
            val y = cy + size.hexSize * sin(angle)

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

private class RenderSize(
    val layoutRadius: Double,
    private val gap: Double,
) {
    companion object {
        val sqrt3 = sqrt(3.0)
    }

    val hexSize get() = layoutRadius - gap

    fun CellCoordinate.toPixel(): Pair<Double, Double> {
        val x = layoutRadius * (sqrt3 * q + sqrt3 / 2 * r)
        val y = layoutRadius * (3.0 / 2 * r)
        return x to y
    }
}

private class BoardRenderer(
    private val boundingBox: BoundingBox,
    private val size: RenderSize,
    private val colorScheme: ColorScheme,
    private val borderThickness: Float,
    private val padding: Int,
) : AutoCloseable {
    val image: BufferedImage
    private val graphics: Graphics2D

    init {
        val width = ceil(boundingBox.maxX - boundingBox.minX + 2 * padding).toInt()
        val height = ceil(boundingBox.maxY - boundingBox.minY + 2 * padding).toInt()

        image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        graphics = image.createGraphics().apply {
            color = colorScheme.background
            fillRect(0, 0, width, height)

            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }
    }

    fun drawCell(position: CellCoordinate, cell: Cell) {
        val (x, y) = size.run { position.toPixel() }
        val hex = createHex(x - boundingBox.minX, y - boundingBox.minY)

        graphics.color = when (cell.owner) {
            Player.X -> colorScheme.playerX
            Player.O -> colorScheme.playerO
            null -> colorScheme.emptyCell
        }
        graphics.fill(hex)

        fun drawHighlight(color: Color) {
            graphics.stroke = BasicStroke(borderThickness * 4)
            graphics.color = color
            graphics.draw(createHex(x - boundingBox.minX, y - boundingBox.minY, inset = borderThickness * 2))
        }

        when {
            cell.highlighted -> {
                graphics.color = colorScheme.highlightedCellBorder.withAlpha(64)
                graphics.fill(hex)

                drawHighlight(colorScheme.highlightedCellBorder)
            }
            cell.focussed -> drawHighlight(colorScheme.focussedCellBorder)
            else -> {
                graphics.stroke = BasicStroke(borderThickness)
                graphics.color = colorScheme.cellBorder
                graphics.draw(hex)
            }
        }
    }

    fun createHex(x: Double, y: Double, inset: Float = 0f): Shape {
        val path = Path2D.Double()

        for (i in 0 until 6) {
            val angle = Math.toRadians(60.0 * i - 30)

            val radius = size.hexSize - inset
            val x = x + padding + radius * cos(angle)
            val y = y + padding + radius * sin(angle)

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        path.closePath()
        return path
    }

    override fun close() {
        graphics.dispose()
    }
}

private fun Color.withAlpha(alpha: Int) = Color(red, green, blue, alpha)
