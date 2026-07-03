package de.mineking.hexo.board.render.image

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

fun HTMLCanvasElement.drawBoard(
    layout: BoardRenderLayout,
    padding: Int,
    offset: Point = Point.Zero,
    theme: Theme = BasicTheme.Default,
    middleLayer: InternalBoardRenderer.() -> Unit = {},
) {
    val context = getContext("2d") as CanvasRenderingContext2D

    context.setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    context.fillStyle = theme.backgroundColor.css
    context.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
    context.translate(padding.toDouble() + offset.x, padding.toDouble() + offset.y)

    CanvasRenderingContext(context).drawBoard(layout, theme, middleLayer)
}

class CanvasRenderingContext(val canvas: CanvasRenderingContext2D) : RenderingContext {
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

    override fun drawPolygon(shape: Polygon, color: Color, outline: Stroke?) {
        canvas.beginPath()
        shape.trace()
        canvas.closePath()

        if (!color.isTransparent()) {
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

    override fun drawString(point: Point, text: String, fontSize: Float, font: FontType, color: Color) {
        val font = when (font) {
            FontType.SansSerifBold -> "800 ${fontSize}px system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif"
            FontType.MonospaceRegular -> "normal ${fontSize}px consolas, monospace, sans-serif"
        }

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

        canvas.fillStyle = color.css
        canvas.fillText(text, textX, textY)
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
