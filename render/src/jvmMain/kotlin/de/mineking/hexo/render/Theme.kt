package de.mineking.hexo.render

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.HighlightLine
import de.mineking.hexo.core.CellOwner
import java.awt.Color

interface Theme {
    data class ElementColors(val backgroundColor: Color, val borderColor: Color)

    val gap: Double
    val borderThickness: Double

    val backgroundColor: Color
    val cellBorderColor: Color
    val highlightColor: ElementColors
    val focusColor: ElementColors

    fun HighlightLine.color(): ElementColors
    fun Cell.backgroundColor(): Color
    fun Cell.labelColor(): Color
}

class BasicTheme(
    override val gap: Double,
    override val borderThickness: Double,
    override val backgroundColor: Color,
    override val cellBorderColor: Color,
    highlightColor: Color,
    focusColor: Color,
    val emptyCellBackgroundColor: Color,
    val emptyCellLabelColor: Color,
    val playerXColor: Color,
    val playerOColor: Color,
) : Theme {
    override val highlightColor = Theme.ElementColors(highlightColor.withAlpha(48), highlightColor)
    override val focusColor = Theme.ElementColors(focusColor.withAlpha(48), focusColor)

    private fun CellOwner?.color(default: Color, transform: (Color) -> Color = { it }) = when (this) {
        CellOwner.X -> transform(playerXColor)
        CellOwner.O -> transform(playerOColor)
        else -> default
    }

    override fun HighlightLine.color() = Theme.ElementColors(
        color.color(default = highlightColor.borderColor).withAlpha(220),
        cellBorderColor.withAlpha(128),
    )
    override fun Cell.backgroundColor() = owner.color(default = emptyCellBackgroundColor)
    override fun Cell.labelColor() = owner.color(default = emptyCellLabelColor) { it.darker().darker().darker() }

    companion object {
        val Default = BasicTheme(
            6.0,
            2.0,
            backgroundColor = Color(0x0f172a),
            cellBorderColor = Color(0x202a3d),
            highlightColor = Color(0xec6fb1),
            focusColor = Color.WHITE,
            emptyCellBackgroundColor = Color(0, true),
            emptyCellLabelColor = Color(0xb1c1e0),
            playerXColor = Color(0xfbc139),
            playerOColor = Color(0x38bdf8),
        )
    }
}

private fun Color.withAlpha(alpha: Int) = Color(red, green, blue, alpha)
