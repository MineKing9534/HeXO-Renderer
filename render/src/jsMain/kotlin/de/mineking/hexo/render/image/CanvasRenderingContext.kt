package de.mineking.hexo.render.image

import de.mineking.hexo.board.Board
import kotlinx.browser.document
import org.w3c.dom.ALPHABETIC
import org.w3c.dom.BUTT
import org.w3c.dom.CanvasLineCap
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.LEFT
import org.w3c.dom.MITER
import org.w3c.dom.ROUND

fun interface CanvasFont {
    fun getFont(size: Float): String
}
val DefaultCanvasFont = CanvasFont { "800 ${it}px system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif" }

fun HTMLCanvasElement.drawBoard(
    board: Board,
    layoutRadius: Double,
    padding: Int,
    offsetX: Double = 0.0,
    offsetY: Double = 0.0,
    focusWinningRows: Boolean = true,
    theme: Theme = BasicTheme.Default,
    font: CanvasFont = DefaultCanvasFont,
) {
    board.render(layoutRadius, focusWinningRows, theme) {
        val context = getContext("2d") as CanvasRenderingContext2D

        context.setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        context.fillStyle = theme.backgroundColor.css
        context.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
        context.translate(padding.toDouble() + offsetX, padding.toDouble() + offsetY)

        CanvasRenderingContext(context, font)
    }
}

class CanvasRenderingContext(
    val canvas: CanvasRenderingContext2D,
    val font: CanvasFont = DefaultCanvasFont,
) : RenderingContext {
    private data class TextExclusion(
        val text: String,
        val font: String,
        val x: Double,
        val y: Double,
        val margin: Double,
    )

    private val textExclusions = mutableListOf<TextExclusion>()

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        val layer = createLineLayer()

        if (outline != null) {
            layer.context.strokeStyle = outline.color.css
            layer.context.lineWidth = (stroke.width + outline.width).toDouble()
            layer.context.lineCap = CanvasLineCap.ROUND
            layer.context.lineJoin = CanvasLineJoin.ROUND
            layer.context.strokeSegment(from, to)
        }

        layer.context.strokeStyle = stroke.color.css
        layer.context.lineWidth = stroke.width.toDouble()
        layer.context.lineCap = CanvasLineCap.ROUND
        layer.context.lineJoin = CanvasLineJoin.ROUND
        layer.context.strokeSegment(from, to)

        layer.context.cutOutTextExclusions()

        canvas.save()
        canvas.setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        canvas.drawImage(layer.canvas, 0.0, 0.0)
        canvas.restore()
    }

    override fun drawPolygon(shape: Polygon, color: Color?, outline: Stroke?) {
        canvas.beginPath()
        shape.trace()
        canvas.closePath()

        if (color != null) {
            canvas.fillStyle = color.css
            canvas.fill()
        }

        if (outline != null) {
            canvas.strokeStyle = outline.color.css
            canvas.lineWidth = outline.width.toDouble()
            canvas.lineCap = CanvasLineCap.BUTT
            canvas.lineJoin = CanvasLineJoin.MITER
            canvas.stroke()
        }
    }

    override fun drawString(point: Point, text: String, fontSize: Float, color: Color?) {
        val font = font.getFont(fontSize)

        canvas.font = font
        canvas.textAlign = CanvasTextAlign.LEFT
        canvas.textBaseline = CanvasTextBaseline.ALPHABETIC

        val metrics = canvas.measureText(text)
        val ascent = metrics.fontBoundingBoxAscent
        val descent = metrics.fontBoundingBoxDescent
        val textX = point.x - metrics.width / 2.0
        val textY = point.y + (ascent - descent) / 2.0
        val margin = fontSize / 6.0

        textExclusions += TextExclusion(text, font, textX, textY, margin)

        if (color != null) {
            canvas.fillStyle = color.css
            canvas.fillText(text, textX, textY)
        }
    }

    override fun close() {
        // Nothing to do
    }

    private fun CanvasRenderingContext2D.strokeSegment(from: Point, to: Point) {
        beginPath()
        moveTo(from.x, from.y)
        lineTo(to.x, to.y)
        stroke()
    }

    private fun createLineLayer(): LineLayer {
        val layerCanvas = document.createElement("canvas") as HTMLCanvasElement
        layerCanvas.width = canvas.canvas.width
        layerCanvas.height = canvas.canvas.height

        val layerContext = layerCanvas.getContext("2d") as CanvasRenderingContext2D
        layerContext.setTransform(canvas.getTransform())

        return LineLayer(layerCanvas, layerContext)
    }

    private fun CanvasRenderingContext2D.cutOutTextExclusions() {
        if (textExclusions.isEmpty()) return

        save()
        globalCompositeOperation = "destination-out"

        textAlign = CanvasTextAlign.LEFT
        textBaseline = CanvasTextBaseline.ALPHABETIC
        lineCap = CanvasLineCap.ROUND
        lineJoin = CanvasLineJoin.ROUND

        textExclusions.forEach {
            font = it.font
            lineWidth = it.margin
            strokeText(it.text, it.x, it.y)
            fillText(it.text, it.x, it.y)
        }

        restore()
    }

    private fun Polygon.trace() {
        points.forEachIndexed { i, (x, y) ->
            if (i == 0) {
                canvas.moveTo(x, y)
            } else {
                canvas.lineTo(x, y)
            }
        }
    }

    private data class LineLayer(val canvas: HTMLCanvasElement, val context: CanvasRenderingContext2D)
}

private val Color.css: String get() = "rgba(${red.toInt()}, ${green.toInt()}, ${blue.toInt()}, ${alpha.toInt() / 255.0})"
