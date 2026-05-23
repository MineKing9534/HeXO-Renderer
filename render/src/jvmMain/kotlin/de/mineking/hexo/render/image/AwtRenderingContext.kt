package de.mineking.hexo.render.image

import de.mineking.hexo.board.Board
import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import kotlin.math.ceil

fun Board.renderToImage(
    layoutRadius: Double,
    padding: Int,
    focusWinningRows: Boolean = true,
    theme: Theme = BasicTheme.Default,
): BufferedImage {
    require(cells.isNotEmpty())

    return render(layoutRadius, focusWinningRows, theme) { boundingBox ->
        val width = ceil(boundingBox.maxX - boundingBox.minX + 2 * padding).toInt()
        val height = ceil(boundingBox.maxY - boundingBox.minY + 2 * padding).toInt()

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()

        graphics.color = theme.backgroundColor.awt
        graphics.fillRect(0, 0, width, height)

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.translate(padding, padding)

        object : RenderingContext by AwtRenderingContext(graphics) {
            val image = image

            override fun close() {
                graphics.dispose()
            }
        }
    }.image
}

class AwtRenderingContext(private val graphics: Graphics2D) : RenderingContext {
    companion object {
        private val BOLD_FONT = Font.createFont(Font.TRUETYPE_FONT, javaClass.getResourceAsStream("/fonts/open-sans.extrabold.ttf"))
    }

    private val clip = Area(Rectangle(Int.MIN_VALUE / 2, Int.MIN_VALUE / 2, Int.MAX_VALUE, Int.MAX_VALUE))

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        val segment = Line2D.Double(from.x, from.y, to.x, to.y)

        val innerStroke = BasicStroke(stroke.width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(segment)
        val inner = Area(innerStroke).apply { intersect(clip) }

        graphics.color = stroke.color.awt
        graphics.fill(inner)

        if (outline != null) {
            val outerStroke = BasicStroke(stroke.width + outline.width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(segment)
            val ring = Area(outerStroke).apply {
                subtract(inner)
                intersect(clip)
            }

            graphics.color = outline.color.awt
            graphics.fill(ring)
        }
    }

    override fun drawPolygon(shape: Polygon, color: Color?, outline: Stroke?) {
        val hex = shape.toShape()

        if (color != null) {
            graphics.color = color.awt
            graphics.fill(hex)
        }

        if (outline != null) {
            graphics.color = outline.color.awt
            graphics.stroke = BasicStroke(outline.width)
            graphics.draw(hex)
        }
    }

    override fun drawString(point: Point, text: String, fontSize: Float, color: Color?) {
        graphics.font = BOLD_FONT.deriveFont(Font.BOLD, fontSize)

        val fm = graphics.fontMetrics
        val textX = point.x - fm.stringWidth(text) / 2.0
        val textY = point.y + (fm.ascent - fm.descent) / 2.0

        val layout = TextLayout(text, graphics.font, graphics.fontRenderContext)
        val shape = layout.getOutline(AffineTransform.getTranslateInstance(textX, textY))
        val margin = BasicStroke(fontSize / 6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(shape)

        clip.subtract(Area(shape))
        clip.subtract(Area(margin))

        if (color != null) {
            graphics.color = color.awt
            graphics.fill(shape)
        }
    }

    private fun Polygon.toShape(): Shape {
        val path = Path2D.Double()

        points.forEachIndexed { i, (x, y) ->
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        path.closePath()
        return path
    }

    override fun close() {
        // It is the responsibility of the code that created the graphics object to dispose it
    }
}

private val Color.awt get() = java.awt.Color(rgba, true)
