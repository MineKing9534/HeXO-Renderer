package de.mineking.hexo.board.render.image

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

private const val BRIGHTNESS_FACTOR = 0.35
private fun UByte.darker() = (toDouble() * BRIGHTNESS_FACTOR).coerceIn(0.0, 255.0).toInt().toUByte()
fun Color.darker() = Color.of(
    red.darker(),
    green.darker(),
    blue.darker(),
    alpha,
)

enum class FontType {
    SansSerifBold,
    MonospaceRegular,
}

interface Theme {
    data class ElementColors(val backgroundColor: Color, val borderColor: Color)
    data class Font(val fontSize: Float, val type: FontType)
    data class Label(val value: String, val color: Color, val font: Font)

    val gap: Double
    val borderThickness: Double
    val lineThickness: Double

    val backgroundColor: Color

    fun LineHighlight.color(): ElementColors
    fun Cell.backgroundColor(): ElementColors
    fun Cell.highlightColor(): ElementColors

    fun Cell.label(): Label?

    fun Cell.drawDecoration(point: Point, renderer: InternalBoardRenderer) {}
}

enum class DefaultTheme(val theme: Theme) {
    HDS(BasicTheme.Default),
    HTTTX(HTTTXTheme),
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
    override val lineThickness = 6 * borderThickness

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

    private fun Cell.labelText() = label
        .takeIf { it.isNotBlank() }
        ?: turn?.toString()

    override fun Cell.label() = labelText()?.let { label ->
        Theme.Label(
            value = label,
            color = owner.color(default = emptyCellLabelColor) { it.darker() },
            font = Theme.Font(fontSize = 0.7f, type = FontType.SansSerifBold),
        )
    }

    companion object {
        val Default = BasicTheme(
            gap = 6.0,
            borderThickness = 2.0,
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

object HTTTXTheme : Theme {
    private val BORDER_COlOR = Color.rgb(0x24213a)
    private val BACKGROUND_COLOR = Color.rgb(0x1f1729)

    private val PLAYER_X_CELL_COLOR = Color.rgb(0x5e2f1d)
    private val PLAYER_X_LABEL_COLOR = Color.rgb(0xd25a06)
    private val PLAYER_O_CELL_COLOR = Color.rgb(0x163a65)
    private val PLAYER_O_LABEL_COLOR = Color.rgb(0x0383e1)

    private val OCCUPIED_HIGHLIGHT_COLOR = Color.rgb(0x651527)
    private val EMPTY_HIGHLIGHT_COLOR = Color.rgb(0x491628)
    private val LINE_HIGHLIGHT_COLOR = Color.rgba(0xaa801628)

    override val gap = 3.0
    override val borderThickness = 0.0
    override val lineThickness = 16.0
    override val backgroundColor = BORDER_COlOR // cells have no border and non-transparent background -> background becomes effective border

    override fun LineHighlight.color() = Theme.ElementColors(LINE_HIGHLIGHT_COLOR, Color.Transparent)

    override fun Cell.backgroundColor() = Theme.ElementColors(
        backgroundColor = when (owner) {
            CellOwner.X -> PLAYER_X_CELL_COLOR
            CellOwner.O -> PLAYER_O_CELL_COLOR
            null -> BACKGROUND_COLOR
        },
        borderColor = Color.Transparent,
    )

    override fun Cell.highlightColor() = Theme.ElementColors(
        backgroundColor = when {
            highlight == null -> Color.Transparent
            owner == null -> EMPTY_HIGHLIGHT_COLOR
            else -> OCCUPIED_HIGHLIGHT_COLOR
        },
        borderColor = Color.Transparent,
    )

    private fun Cell.labelText() = label
        .takeIf { it.isNotBlank() }
        ?: turn?.let { (it + 1) / 2 }?.toString()

    override fun Cell.label() = labelText()?.let { label ->
        Theme.Label(
            value = label,
            color = when (owner) {
                CellOwner.X -> PLAYER_X_LABEL_COLOR
                CellOwner.O -> PLAYER_O_LABEL_COLOR
                null -> OCCUPIED_HIGHLIGHT_COLOR
            },
            font = Theme.Font(fontSize = 0.85f, type = FontType.MonospaceRegular),
        )
    }

    override fun Cell.drawDecoration(point: Point, renderer: InternalBoardRenderer) {
        if (turn != null) return

        val thickness = renderer.lineThickness * 0.6f

        when (owner) {
            CellOwner.X -> {
                fun drawXPart(direction: Point) {
                    renderer.renderingContext.drawLine(
                        from = point - direction,
                        to = point + direction,
                        stroke = Stroke(PLAYER_X_LABEL_COLOR, thickness),
                    )
                }

                val delta = renderer.hexSize * 0.42
                drawXPart(Point(delta, delta))
                drawXPart(Point(delta, -delta))
            }
            CellOwner.O -> {
                fun drawCircle(radius: Double, color: Color) {
                    renderer.renderingContext.drawLine(
                        from = point,
                        to = point,
                        stroke = Stroke(color, radius.toFloat()),
                    )
                }

                val radius = renderer.hexSize * 1.2
                drawCircle(radius, PLAYER_O_LABEL_COLOR)

                val oldColor = if (highlight != null) OCCUPIED_HIGHLIGHT_COLOR else PLAYER_O_CELL_COLOR
                drawCircle(radius - thickness * 2, oldColor)
            }
            else -> return
        }
    }
}
