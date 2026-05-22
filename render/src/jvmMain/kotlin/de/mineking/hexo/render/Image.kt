package de.mineking.hexo.render

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.HighlightLine
import de.mineking.hexo.board.end
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
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

private fun BufferedImage.toByteArray(): ByteArray {
    val output = ByteArrayOutputStream()
    ImageIO.write(this, "png", output)
    return output.toByteArray()
}

context(renderer: BoardRenderer<BufferedImage>)
suspend fun Board.renderToByteArray() = renderer.run { render().toByteArray() }

class ImageBoardRenderer(
    private val layoutRadius: Double,
    private val padding: Int,
    private val focusWinningRows: Boolean = true,
    private val theme: Theme = BasicTheme.Default,
) : BoardRenderer<BufferedImage> {
    companion object {
        val Default = ImageBoardRenderer(
            layoutRadius = 64.0,
            padding = 32,
        )
    }

    override suspend fun Board.render() = renderToImage(
        layoutRadius = layoutRadius,
        padding = padding,
        focusWinningRows = focusWinningRows,
        theme = theme,
    )
}

fun Board.renderToImage(
    layoutRadius: Double,
    padding: Int,
    focusWinningRows: Boolean = true,
    theme: Theme = BasicTheme.Default,
): BufferedImage {
    require(cells.isNotEmpty())

    val size = RenderSize(layoutRadius)
    val boundingBox = findBoundingBox(size)
    val renderer = InternalBoardRenderer(
        boundingBox,
        size,
        padding,
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

        cells.forEach { (position, cell) -> renderer.drawCellLabel(position, cell) }

        return renderer.image
    }
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
        val (cx, cy) = size.run { position.toPixel() }

        for (i in 0 until 6) {
            val angle = Math.toRadians(60.0 * i - 30)
            val x = cx + size.layoutRadius * cos(angle)
            val y = cy + size.layoutRadius * sin(angle)

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

    fun CellCoordinate.toPixel(): Pair<Double, Double> {
        val x = layoutRadius * (sqrt3 * q + sqrt3 / 2 * r)
        val y = layoutRadius * (3.0 / 2 * r)
        return x to y
    }
}

private class InternalBoardRenderer(
    private val boundingBox: BoundingBox,
    private val size: RenderSize,
    private val padding: Int,
    private val theme: Theme,
) : AutoCloseable {
    companion object {
        private val BOLD_FONT = Font.createFont(Font.TRUETYPE_FONT, javaClass.getResourceAsStream("/fonts/open-sans.extrabold.ttf"))
    }

    private val hexSize = size.layoutRadius * (1 - theme.gap / 64)
    private val borderThickness = (size.layoutRadius * theme.borderThickness / 64).toFloat()

    val image: BufferedImage
    private val graphics: Graphics2D

    private val cellColors = mutableMapOf<Cell, Color>()

    init {
        val width = ceil(boundingBox.maxX - boundingBox.minX + 2 * padding).toInt()
        val height = ceil(boundingBox.maxY - boundingBox.minY + 2 * padding).toInt()

        image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        graphics = image.createGraphics().apply {
            color = theme.backgroundColor
            fillRect(0, 0, width, height)

            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }
    }

    fun drawLine(line: HighlightLine) {
        val (sx, sy) = line.start.toPixel()
        val (ex, ey) = line.end.toPixel()

        val segment = Line2D.Double(sx, sy, ex, ey)

        val innerStroke = BasicStroke(borderThickness * 6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(segment)
        val outerStroke = BasicStroke(borderThickness * 8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(segment)

        val inner = Area(innerStroke)
        val ring = Area(outerStroke).apply { subtract(inner) }

        val (backgroundColor, borderColor) = theme.run { line.color() }

        graphics.color = borderColor
        graphics.fill(ring)

        graphics.color = backgroundColor
        graphics.fill(inner)
    }

    fun drawCell(position: CellCoordinate, cell: Cell) {
        val (x, y) = position.toPixel()
        val hex = createHex(x, y)

        graphics.color = theme.run { cell.backgroundColor() }
        graphics.fill(hex)

        fun drawHighlight(color: Theme.ElementColors) {
            graphics.color = color.backgroundColor
            graphics.fill(hex)

            graphics.stroke = BasicStroke(borderThickness * 3)
            graphics.color = color.borderColor
            graphics.draw(hex)
        }

        when {
            cell.highlighted -> drawHighlight(theme.highlightColor)
            cell.focussed -> drawHighlight(theme.focusColor)
            else -> {
                graphics.stroke = BasicStroke(borderThickness)
                graphics.color = theme.cellBorderColor
                graphics.draw(hex)
            }
        }

        if (x.toInt() in 0 until image.width && y.toInt() in 0 until image.height) {
            cellColors[cell] = Color(image.getRGB(x.toInt(), y.toInt()), false)
        }
    }

    fun drawCellLabel(position: CellCoordinate, cell: Cell) {
        val (x, y) = position.toPixel()
        val text = cell.label.takeIf { it.isNotBlank() }
            ?: cell.turn?.toString()
            ?: return

        val fontSize = hexSize.toFloat() * 0.7f
        graphics.font = BOLD_FONT.deriveFont(Font.BOLD, fontSize)

        val fm = graphics.fontMetrics
        val textX = x - fm.stringWidth(text) / 2.0
        val textY = y + (fm.ascent - fm.descent) / 2.0

        val layout = TextLayout(text, graphics.font, graphics.fontRenderContext)
        val shape = layout.getOutline(AffineTransform.getTranslateInstance(textX, textY))

        val cellColor = cellColors[cell]
        if (cellColor != null) {
            graphics.color = cellColor
            graphics.stroke = BasicStroke(fontSize / 6)
            graphics.draw(shape)
        }

        graphics.color = theme.run { cell.labelColor() }
        graphics.fill(shape)
    }

    fun createHex(x: Double, y: Double, inset: Float = 0f): Shape {
        val path = Path2D.Double()

        for (i in 0 until 6) {
            val angle = Math.toRadians(60.0 * i - 30)

            val radius = hexSize - inset
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
