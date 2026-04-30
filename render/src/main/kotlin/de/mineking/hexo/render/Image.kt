@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.render

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Cell
import de.mineking.hexo.core.CellCoordinate
import de.mineking.hexo.core.HighlightLine
import de.mineking.hexo.core.Player
import de.mineking.hexo.core.end
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.Area
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
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

private fun BufferedImage.toByteArray(): ByteArray {
    val output = ByteArrayOutputStream()
    ImageIO.write(this, "png", output)
    return output.toByteArray()
}

context(renderer: BoardRenderer<BufferedImage>)
suspend fun Board.renderToByteArray() = renderer.run { render().toByteArray() }

class ImageBoardRenderer(
    private val layoutRadius: Double,
    private val gap: Double,
    private val borderThickness: Float,
    private val padding: Int,
    private val focusWinningRows: Boolean = true,
    private val colorScheme: ColorScheme = ColorScheme.Default,
) : BoardRenderer<BufferedImage> {
    companion object {
        val Default = ImageBoardRenderer(
            layoutRadius = 64.0,
            gap = 6.0,
            borderThickness = 2f,
            padding = 32,
        )
    }

    override suspend fun Board.render() = renderToImage(
        layoutRadius = layoutRadius,
        gap = gap,
        borderThickness = borderThickness,
        padding = padding,
        focusWinningRows = focusWinningRows,
        colorScheme = colorScheme,
    )
}

fun Board.renderToImage(
    layoutRadius: Double,
    gap: Double,
    borderThickness: Float,
    padding: Int,
    focusWinningRows: Boolean = true,
    colorScheme: ColorScheme = ColorScheme.Default,
): BufferedImage {
    require(cells.isNotEmpty())

    val size = RenderSize(layoutRadius, gap)
    val boundingBox = findBoundingBox(size)
    val renderer = InternalBoardRenderer(
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

    highlightedLines.forEach {
        renderer.drawLine(it)
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

private fun Board.findBoundingBox(size: RenderSize): BoundingBox {
    var minX = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY

    val endPoints = highlightedLines.flatMap { listOf(it.start, it.end) }
    for (position in cells.keys + endPoints) {
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

private class InternalBoardRenderer(
    private val boundingBox: BoundingBox,
    private val size: RenderSize,
    private val colorScheme: ColorScheme,
    private val borderThickness: Float,
    private val padding: Int,
) : AutoCloseable {
    companion object {
        private val BOLD_FONT = Font.createFont(Font.TRUETYPE_FONT, javaClass.getResourceAsStream("/fonts/open-sans.extrabold.ttf"))
    }

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

    private fun Player?.color(emptyCellColor: Color) = when (this) {
        Player.X -> colorScheme.playerX
        Player.O -> colorScheme.playerO
        null -> emptyCellColor
    }

    fun drawLine(line: HighlightLine) {
        val (sx, sy) = line.start.toPixel()
        val (ex, ey) = line.end.toPixel()

        val segment = Line2D.Double(sx, sy, ex, ey)

        val innerStroke = BasicStroke(borderThickness * 6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(segment)
        val outerStroke = BasicStroke(borderThickness * 8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(segment)

        val inner = Area(innerStroke)
        val ring = Area(outerStroke).apply { subtract(inner) }
        graphics.color = colorScheme.cellBorder.withAlpha(128)
        graphics.fill(ring)

        graphics.color = line.color.color(emptyCellColor = colorScheme.highlightedCellBorder).withAlpha(220)
        graphics.fill(inner)
    }

    fun drawCell(position: CellCoordinate, cell: Cell) {
        val (x, y) = position.toPixel()
        val hex = createHex(x, y)

        graphics.color = cell.owner.color(emptyCellColor = colorScheme.emptyCell)
        graphics.fill(hex)

        fun drawHighlight(color: Color) {
            graphics.color = color.withAlpha(48)
            graphics.fill(hex)

            graphics.stroke = BasicStroke(borderThickness * 3)
            graphics.color = color
            graphics.draw(hex)
        }

        when {
            cell.highlighted -> drawHighlight(colorScheme.highlightedCellBorder)
            cell.focussed -> drawHighlight(colorScheme.focussedCellBorder)
            else -> {
                graphics.stroke = BasicStroke(borderThickness)
                graphics.color = colorScheme.cellBorder
                graphics.draw(hex)
            }
        }

        drawTurnNumber(cell, x, y)
    }

    private fun drawTurnNumber(cell: Cell, x: Double, y: Double) {
        val text = cell.turn?.toString() ?: return

        val oldFont = graphics.font
        graphics.font = BOLD_FONT.deriveFont(Font.BOLD, size.hexSize.toFloat() * 0.7f)
        graphics.color = cell.owner.color(emptyCellColor = colorScheme.emptyCell)

        val fm = graphics.fontMetrics
        val textX = x - fm.stringWidth(text) / 2.0
        val textY = y + (fm.ascent - fm.descent) / 2.0

        graphics.drawString(text, textX.toFloat(), textY.toFloat())
        graphics.font = oldFont
    }

    fun createHex(x: Double, y: Double, inset: Float = 0f): Shape {
        val path = Path2D.Double()

        for (i in 0 until 6) {
            val angle = Math.toRadians(60.0 * i - 30)

            val radius = size.hexSize - inset
            val x = x + radius * cos(angle)
            val y = y + radius * sin(angle)

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        path.closePath()
        return path
    }

    override fun close() {
        graphics.dispose()
    }

    private fun CellCoordinate.toPixel() = size.run {
        toPixel().let { (x, y) ->
            x - boundingBox.minX + padding to y - boundingBox.minY + padding
        }
    }
}

private fun Color.withAlpha(alpha: Int) = Color(red, green, blue, alpha)
