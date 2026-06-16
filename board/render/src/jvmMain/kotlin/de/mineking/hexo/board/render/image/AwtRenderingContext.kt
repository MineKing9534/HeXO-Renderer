package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Board
import java.awt.AlphaComposite
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

fun Board.renderToImage(
    layoutRadius: Double,
    padding: Int,
    theme: Theme = BasicTheme.Default,
    middleLayer: InternalBoardRenderer.() -> Unit = {},
): BufferedImage {
    require(cells.isNotEmpty() || lineHighlights.isNotEmpty())

    val layout = createRenderLayout(layoutRadius, BoardRenderBounds.Compact)
    val width = layout.boundingBox.width + 2 * padding
    val height = layout.boundingBox.height + 2 * padding

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()

    graphics.color = theme.backgroundColor.awt
    graphics.fillRect(0, 0, width, height)

    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    graphics.translate(padding - layout.boundingBox.minX, padding - layout.boundingBox.minY)

    val context = AwtRenderingContext(graphics)
    try {
        context.drawBoard(layout.copy(boundingBox = layout.boundingBox.pad(padding)), theme, middleLayer)
    } finally {
        graphics.dispose()
    }

    return image
}

class AwtRenderingContext(private val graphics: Graphics2D) : RenderingContext {
    companion object {
        private val BOLD_FONT = Font.createFont(Font.TRUETYPE_FONT, javaClass.getResourceAsStream("/fonts/open-sans.extrabold.ttf"))
        private val NORMAL_FONT = Font.createFont(Font.TRUETYPE_FONT, javaClass.getResourceAsStream("/fonts/open-sans.regular.ttf"))
        const val TEXT_MARGIN_ALPHA = 0.2f
    }

    private val clip = Area(Rectangle(Int.MIN_VALUE / 2, Int.MIN_VALUE / 2, Int.MAX_VALUE, Int.MAX_VALUE))
    private val textMargins = Area()

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        val segment = Line2D.Double(from.x, from.y, to.x, to.y)

        val inner = segment.stroke(stroke.width)
        drawLinePart(inner, stroke.color)

        if (outline != null) {
            val ring = segment.stroke(stroke.width + outline.width).apply {
                subtract(inner)
            }

            drawLinePart(ring, outline.color)
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

    override fun drawString(point: Point, text: String, fontSize: Float, bold: Boolean, color: Color) {
        graphics.font = when {
            bold -> BOLD_FONT.deriveFont(Font.BOLD, fontSize)
            else -> NORMAL_FONT.deriveFont(fontSize)
        }

        val fm = graphics.fontMetrics
        val textX = point.x - fm.stringWidth(text) / 2.0
        val textY = point.y + (fm.ascent - fm.descent) / 2.0

        val layout = TextLayout(text, graphics.font, graphics.fontRenderContext)
        val shape = layout.getOutline(AffineTransform.getTranslateInstance(textX, textY))
        val margin = BasicStroke(fontSize / 6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(shape)

        clip.subtract(Area(shape))
        textMargins.add(Area(margin).apply {
            subtract(Area(shape))
        })

        graphics.color = color.awt
        graphics.fill(shape)
    }

    private fun drawLinePart(shape: Area, color: Color) {
        val visible = Area(shape).apply { intersect(clip) }
        val margin = Area(visible).apply { intersect(textMargins) }
        visible.subtract(margin)

        graphics.color = color.awt
        graphics.fill(visible)

        val composite = graphics.composite
        graphics.composite = AlphaComposite.SrcOver.derive(TEXT_MARGIN_ALPHA)

        graphics.fill(margin)
        graphics.composite = composite
    }

    private fun Line2D.Double.stroke(width: Float) = Area(
        BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(this),
    )

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
}

private val Color.awt get() = java.awt.Color(rgba, true)
