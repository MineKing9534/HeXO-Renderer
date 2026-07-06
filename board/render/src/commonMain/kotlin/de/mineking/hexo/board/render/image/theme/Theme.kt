package de.mineking.hexo.board.render.image.theme

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.createHex
import kotlin.jvm.JvmInline

@JvmInline
value class Color private constructor(val rgba: Int) {
    val alpha get() = (rgba shr 3 * 8 and 0xff).toUByte()
    val red get() = (rgba shr 2 * 8 and 0xff).toUByte()
    val green get() = (rgba shr 1 * 8 and 0xff).toUByte()
    val blue get() = (rgba shr 0 * 8 and 0xff).toUByte()

    companion object {
        val Transparent = rgba(0)

        fun rgb(rgb: Int) = Color(rgb or 0xff000000.toInt())
        fun rgba(rgba: Long) = Color(rgba.toInt())

        fun of(red: UByte, green: UByte, blue: UByte, alpha: UByte) = Color(
            rgba = (alpha.toInt() shl (3 * 8)) +
                (red.toInt() shl (2 * 8)) +
                (green.toInt() shl (1 * 8)) +
                (blue.toInt() shl (0 * 8)),
        )
    }

    override fun toString() = "#${(rgba and 0xffffff).toHexString(HexFormat {
        number {
            minLength = 6
            removeLeadingZeros = true
        }
    })}${alpha.toHexString()}"
}

fun Color.isTransparent() = alpha == 0.toUByte()

fun Color.withAlpha(alpha: Int) = Color.rgba((((alpha and 0xff) shl (3 * 8)) or (rgba and 0xffffff)).toLong())

private fun UByte.adjustBrightness(factor: Double) = (toDouble() * factor).coerceIn(0.0, 255.0).toInt().toUByte()

fun Color.adjustBrightness(factor: Double) = Color.of(
    red.adjustBrightness(factor),
    green.adjustBrightness(factor),
    blue.adjustBrightness(factor),
    alpha,
)

fun Color.darker() = adjustBrightness(0.35)
fun Color.brighter() = adjustBrightness(2.85)

enum class FontType {
    SansSerifBold,
    MonospaceRegular,
}

abstract class Theme(
    open val gap: Double,
    open val backgroundColor: Color,
) {
    abstract fun render(context: RenderingContext, middleLayer: () -> Unit)

    companion object {
        val Default: Theme get() = HDSTheme.Default
    }
}

abstract class BaseTheme(
    override val gap: Double,
    override val backgroundColor: Color,
) : Theme(gap, backgroundColor) {
    interface Renderer {
        fun drawCell(point: Point, hex: Polygon, cell: Cell)
        fun drawLineHighlight(lineHighlight: LineHighlight)
    }

    protected abstract fun renderer(context: RenderingContext): Renderer

    protected fun Renderer.render(context: RenderingContext, middleLayer: () -> Unit) = context.run {
        context.layout.coordinates.forEach {
            val point = it.toPixel()
            val hex = point.createHex(context.hexSize)

            if (!hex.isVisible()) return@forEach

            val cell = context.layout.board.cells[it] ?: Cell.EMPTY
            drawCell(point, hex, cell)
        }

        middleLayer()
        context.layout.board.lineHighlights.forEach {
            drawLineHighlight(it)
        }
    }

    override fun render(context: RenderingContext, middleLayer: () -> Unit) {
        val renderer = renderer(context)
        renderer.render(context, middleLayer)
    }
}

enum class DefaultTheme(val theme: Theme) {
    HDS(HDSTheme.Default),
    HTTTX(HTTTXTheme.Default),
    Tyto(TytoTheme.Default),
}
