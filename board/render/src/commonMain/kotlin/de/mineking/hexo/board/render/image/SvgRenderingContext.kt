package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Board
import dev.jamesyox.svg4k.SvgTag
import dev.jamesyox.svg4k.SvgTagDSL
import dev.jamesyox.svg4k.TagConsumer
import dev.jamesyox.svg4k.attr.AttributeConsumer
import dev.jamesyox.svg4k.attr.AttributeContainer
import dev.jamesyox.svg4k.attr.attrs.DominantBaseline
import dev.jamesyox.svg4k.attr.attrs.FontSize
import dev.jamesyox.svg4k.attr.attrs.FontWeight
import dev.jamesyox.svg4k.attr.attrs.MaskType
import dev.jamesyox.svg4k.attr.attrs.MaskUnits
import dev.jamesyox.svg4k.attr.attrs.StrokeLinecap
import dev.jamesyox.svg4k.attr.attrs.StrokeLinejoin
import dev.jamesyox.svg4k.attr.attrs.TextAnchor
import dev.jamesyox.svg4k.attr.attrs.ViewBox
import dev.jamesyox.svg4k.attr.attrs.cx
import dev.jamesyox.svg4k.attr.attrs.cy
import dev.jamesyox.svg4k.attr.attrs.dominantBaseline
import dev.jamesyox.svg4k.attr.attrs.fill
import dev.jamesyox.svg4k.attr.attrs.fontFamily
import dev.jamesyox.svg4k.attr.attrs.fontWeight
import dev.jamesyox.svg4k.attr.attrs.id
import dev.jamesyox.svg4k.attr.attrs.mask
import dev.jamesyox.svg4k.attr.attrs.maskType
import dev.jamesyox.svg4k.attr.attrs.maskUnits
import dev.jamesyox.svg4k.attr.attrs.points
import dev.jamesyox.svg4k.attr.attrs.r
import dev.jamesyox.svg4k.attr.attrs.stroke
import dev.jamesyox.svg4k.attr.attrs.strokeLinecap
import dev.jamesyox.svg4k.attr.attrs.strokeLinejoin
import dev.jamesyox.svg4k.attr.attrs.strokeWidth
import dev.jamesyox.svg4k.attr.attrs.textAnchor
import dev.jamesyox.svg4k.attr.attrs.viewBox
import dev.jamesyox.svg4k.attr.attrs.x
import dev.jamesyox.svg4k.attr.attrs.x1
import dev.jamesyox.svg4k.attr.attrs.x2
import dev.jamesyox.svg4k.attr.attrs.y
import dev.jamesyox.svg4k.attr.attrs.y1
import dev.jamesyox.svg4k.attr.attrs.y2
import dev.jamesyox.svg4k.attr.types.obj.SvgColor
import dev.jamesyox.svg4k.attr.types.obj.SvgId
import dev.jamesyox.svg4k.attr.types.obj.none
import dev.jamesyox.svg4k.attr.types.obj.pct
import dev.jamesyox.svg4k.attr.types.obj.px
import dev.jamesyox.svg4k.consumers.svgString
import dev.jamesyox.svg4k.tags.Mask
import dev.jamesyox.svg4k.tags.categories.container.AllElementContainer
import dev.jamesyox.svg4k.tags.categories.container.ElementContainer
import dev.jamesyox.svg4k.tags.categories.container.unaryPlus
import dev.jamesyox.svg4k.tags.circle
import dev.jamesyox.svg4k.tags.defs
import dev.jamesyox.svg4k.tags.line
import dev.jamesyox.svg4k.tags.mask
import dev.jamesyox.svg4k.tags.polygon
import dev.jamesyox.svg4k.tags.rect
import dev.jamesyox.svg4k.tags.svg
import dev.jamesyox.svg4k.tags.text
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import dev.jamesyox.svg4k.attr.attrs.fontSize as fs
import dev.jamesyox.svg4k.attr.attrs.height as h
import dev.jamesyox.svg4k.attr.attrs.width as w
import dev.jamesyox.svg4k.attr.types.obj.Point as SvgPoint

fun Board.renderToSvg(
    padding: Int,
    layoutRadius: Double = 64.0,
    prettyPrint: Boolean = false,
    theme: Theme = BasicTheme.Default,
    middleLayer: InternalBoardRenderer.() -> Unit = {},
): String {
    require(cells.isNotEmpty())

    val layout = createRenderLayout(layoutRadius, BoardRenderBounds.Compact)
    val width = layout.boundingBox.width + 2 * padding
    val height = layout.boundingBox.height + 2 * padding

    return svgString(prettyPrint) {
        svg {
            val topLeftCorner = layout.boundingBox.topLeft - Point(padding, padding)

            viewBox = ViewBox(topLeftCorner.x, topLeftCorner.y, width, height)
            rect {
                x = topLeftCorner.x.none
                y = topLeftCorner.y.none
                w = 100.pct
                h = 100.pct
                fill(theme.backgroundColor.svg)
            }

            group {
                val context = SvgRenderingContext(topLeftCorner) { it() }
                context.drawBoard(layout.copy(boundingBox = layout.boundingBox.pad(padding)), theme, middleLayer)

                context.finish()
            }
        }
    }
}

class SvgRenderingContext(
    private val topLeftCorner: Point,
    private val configure: (context(TagConsumer<*>, @SvgTagDSL Group) () -> Unit) -> Unit,
) : RenderingContext {
    companion object {
        private val FULL_MASK = SvgId("full-mask")
        private val TEXT_MASK = SvgId("text-mask")
    }

    @Suppress("ContextReceiverMapping")
    private val textMask = mutableListOf<context(TagConsumer<*>, @SvgTagDSL Mask) () -> Unit>()

    context(_: TagConsumer<*>, _: ElementContainer.Defs)
    fun finish() {
        defs {
            fun createMask(name: SvgId, config: context(AttributeConsumer, @SvgTagDSL Mask) () -> Unit = {}) {
                mask {
                    x = topLeftCorner.x.none
                    y = topLeftCorner.y.none
                    w = 100.pct
                    h = 100.pct

                    id = name
                    maskType = MaskType.Luminance
                    maskUnits = MaskUnits.UserSpaceOnUse

                    rect {
                        x = topLeftCorner.x.none
                        y = topLeftCorner.y.none
                        w = 100.pct
                        h = 100.pct

                        fill(Color.rgb(0xffffff).svg)
                    }

                    config()
                }
            }

            createMask(FULL_MASK)
            createMask(TEXT_MASK) {
                textMask.forEach { it() }
            }
        }
    }

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        configure {
            fun drawLinePart(stroke: Stroke) {
                // For some reason drawing a line with the same start and end point doesn't work properly
                if (from == to) {
                    circle {
                        cx = from.x.none
                        cy = from.y.none

                        fill(stroke.color.svg)
                        r = (stroke.width / 2).none

                        mask(TEXT_MASK)
                    }
                } else {
                    line {
                        x1 = from.x.none
                        y1 = from.y.none
                        x2 = to.x.none
                        y2 = to.y.none

                        stroke(stroke.color.svg)
                        strokeWidth = stroke.width.none
                        strokeLinecap = StrokeLinecap.Round

                        mask(TEXT_MASK)
                    }
                }
            }

            if (outline != null) drawLinePart(Stroke(outline.color, stroke.width + outline.width))
            drawLinePart(stroke)
        }
    }

    override fun drawPolygon(shape: Polygon, color: Color?, outline: Stroke?) {
        configure {
            polygon {
                mask(FULL_MASK)
                points = shape.points.map { (x, y) -> SvgPoint(x, y) }

                if (color != null) fill(color.svg)
                if (outline != null) {
                    stroke(outline.color.svg)
                    strokeWidth = outline.width.none
                }
            }
        }
    }

    override fun drawString(point: Point, text: String, fontSize: Float, color: Color) {
        context(_: TagConsumer<*>, _: ElementContainer.Text)
        fun drawText(color: Color?, stroke: Stroke?) {
            text {
                if (color != null) fill(color.svg)
                if (stroke != null) {
                    stroke(stroke.color.svg)
                    strokeWidth = stroke.width.none
                    strokeLinecap = StrokeLinecap.Round
                    strokeLinejoin = StrokeLinejoin.Round
                }

                fontWeight = FontWeight.Numeric(800)
                fontFamily = "system-ui, -apple-system, BlinkMacSystemFont, sans-serif"
                fs = FontSize.Value(fontSize.px)

                textAnchor = TextAnchor.Middle
                dominantBaseline = DominantBaseline.Middle

                x = listOf(point.x.none)
                y = listOf(point.y.none)

                +text
            }
        }

        configure { drawText(color, null) }
        textMask += {
            drawText(null, Stroke(Color.rgb(0x333333), fontSize / 6))
            drawText(Color.rgb(0x000000), null)
        }
    }
}

@OptIn(ExperimentalContracts::class)
@IgnorableReturnValue
context(tagConsumer: TagConsumer<T>, _: ElementContainer.Text)
private fun <T> group(content: context(AttributeConsumer, @SvgTagDSL Group) () -> Unit): T {
    contract {
        callsInPlace(content, InvocationKind.EXACTLY_ONCE)
    }

    tagConsumer.onTagStart(Group)
    content(tagConsumer.attributeConsumer, Group)
    tagConsumer.onTagEnd(Group)
    return tagConsumer.output()
}

object Group :
    SvgTag,
    AllElementContainer,
    AttributeContainer.Transform {
    override val tagName = "g"
}

private val Color.svg get() = SvgRgbaColor(this)
private class SvgRgbaColor(color: Color) : SvgColor {
    override val svgString = "rgba(${color.red}, ${color.green}, ${color.blue}, ${color.alpha.toInt() / 255.0})"
}
