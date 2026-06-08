package de.mineking.hexo.render.image

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.core.CellOwner
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
        fun rgba(rgba: Int) = Color(rgba)

        fun of(red: UByte, green: UByte, blue: UByte, alpha: UByte) = Color(
            rgba = (alpha.toInt() shl (3 * 8)) +
                (red.toInt() shl (2 * 8)) +
                (green.toInt() shl (1 * 8)) +
                (blue.toInt() shl (0 * 8)),
        )
    }

    override fun toString() = "#${rgba.toHexString()}"
}

fun Color.isTransparent() = alpha == 0.toUByte()

fun Color.withAlpha(alpha: Int) = Color.rgba(((alpha and 0xff) shl (3 * 8)) or (rgba and 0xffffff))

private const val BRIGHTNESS_FACTOR = 0.35
private fun UByte.darker() = (toDouble() * BRIGHTNESS_FACTOR).coerceIn(0.0, 255.0).toInt().toUByte()
fun Color.darker() = Color.of(
    red.darker(),
    green.darker(),
    blue.darker(),
    alpha,
)

interface Theme {
    data class ElementColors(val backgroundColor: Color, val borderColor: Color)

    val gap: Double
    val borderThickness: Double

    val backgroundColor: Color

    fun LineHighlight.color(): ElementColors
    fun Cell.backgroundColor(): ElementColors
    fun Cell.highlightColor(): ElementColors

    fun Cell.labelColor(): Color
}

data class BasicTheme(
    override val gap: Double,
    override val borderThickness: Double,
    override val backgroundColor: Color,
    val cellBorderColor: Color,
    val highlightColor: Color,
    val focusColor: Color,
    val emptyCellBackgroundColor: Color,
    val emptyCellLabelColor: Color,
    val playerXColor: Color,
    val playerOColor: Color,
) : Theme {
    private fun CellOwner?.color(default: Color, transform: (Color) -> Color = { it }) = when (this) {
        CellOwner.X -> transform(playerXColor)
        CellOwner.O -> transform(playerOColor)
        else -> default
    }

    override fun LineHighlight.color() = Theme.ElementColors(
        color.color(default = highlightColor).withAlpha(240),
        cellBorderColor.withAlpha(128),
    )
    override fun Cell.backgroundColor() = Theme.ElementColors(owner.color(default = emptyCellBackgroundColor), cellBorderColor)
    override fun Cell.highlightColor(): Theme.ElementColors {
        val (color, fillBackground) = when {
            highlight != null -> highlight?.color.color(default = highlightColor) to (highlight?.color == null)
            focused -> focusColor to true
            else -> return Theme.ElementColors(Color.Transparent, Color.Transparent)
        }

        val alpha = if (fillBackground) 48 else 16
        return Theme.ElementColors(color.withAlpha(alpha), color)
    }

    override fun Cell.labelColor() = owner.color(default = emptyCellLabelColor) { it.darker() }

    companion object {
        val Default = BasicTheme(
            6.0,
            2.0,
            backgroundColor = Color.rgb(0x0f172a),
            cellBorderColor = Color.rgb(0x232d43),
            highlightColor = Color.rgb(0xec6fb1),
            focusColor = Color.rgb(0xffffff),
            emptyCellBackgroundColor = Color.Transparent,
            emptyCellLabelColor = Color.rgb(0xb1c1e0),
            playerXColor = Color.rgb(0xfbc139),
            playerOColor = Color.rgb(0x38bdf8),
        )
    }
}
