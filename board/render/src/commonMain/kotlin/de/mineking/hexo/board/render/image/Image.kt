package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.render.image.theme.Theme
import kotlin.math.cos
import kotlin.math.sin

@IgnorableReturnValue
fun RenderingBackend.drawBoard(
    layout: BoardRenderLayout,
    theme: Theme = Theme.Default,
    middleLayer: RenderingContext.() -> Unit = {},
) {
    val context = RenderingContext(
        backend = this,
        layout = layout,
        theme = theme,
    )

    theme.render(context) {
        context.middleLayer()
    }
}

class RenderingContext(
    val backend: RenderingBackend,
    val layout: BoardRenderLayout,
    theme: Theme,
) {
    val maxTurn = layout.board.cells.values
        .maxOfOrNull { it.turn ?: Int.MIN_VALUE }
        ?.takeIf { it > Int.MIN_VALUE }

    val hexSize = layout.size.layoutRadius * (1 - theme.gap / 64)

    fun Polygon.isVisible() = points.any { it in layout.boundingBox }
    fun CellCoordinate.toPixel() = layout.size.run { toPixel() }

    fun Double.relativeWidth() = (layout.size.layoutRadius * this / 64).toFloat()
}

private const val DEGREES_TO_RADIANS = 0.017453292519943295
fun Point.createHex(radius: Double) = Polygon((0 until 6).map {
    val angle = DEGREES_TO_RADIANS * (60.0 * it - 30)

    val x = x + radius * cos(angle)
    val y = y + radius * sin(angle)

    Point(x, y)
})
