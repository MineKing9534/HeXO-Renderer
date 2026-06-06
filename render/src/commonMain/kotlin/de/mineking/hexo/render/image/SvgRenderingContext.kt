package de.mineking.hexo.render.image

import de.mineking.hexo.board.Board
import dev.jamesyox.svg4k.SvgTag
import dev.jamesyox.svg4k.SvgTagDSL
import dev.jamesyox.svg4k.TagConsumer
import dev.jamesyox.svg4k.attr.AttributeConsumer
import dev.jamesyox.svg4k.attr.AttributeContainer
import dev.jamesyox.svg4k.attr.attrs.DominantBaseline
import dev.jamesyox.svg4k.attr.attrs.FontSize
import dev.jamesyox.svg4k.attr.attrs.FontWeight
import dev.jamesyox.svg4k.attr.attrs.StrokeLinecap
import dev.jamesyox.svg4k.attr.attrs.TextAnchor
import dev.jamesyox.svg4k.attr.attrs.ViewBox
import dev.jamesyox.svg4k.attr.attrs.dominantBaseline
import dev.jamesyox.svg4k.attr.attrs.fill
import dev.jamesyox.svg4k.attr.attrs.fontFamily
import dev.jamesyox.svg4k.attr.attrs.fontWeight
import dev.jamesyox.svg4k.attr.attrs.points
import dev.jamesyox.svg4k.attr.attrs.stroke
import dev.jamesyox.svg4k.attr.attrs.strokeLinecap
import dev.jamesyox.svg4k.attr.attrs.strokeWidth
import dev.jamesyox.svg4k.attr.attrs.textAnchor
import dev.jamesyox.svg4k.attr.attrs.transform
import dev.jamesyox.svg4k.attr.attrs.viewBox
import dev.jamesyox.svg4k.attr.attrs.x
import dev.jamesyox.svg4k.attr.attrs.x1
import dev.jamesyox.svg4k.attr.attrs.x2
import dev.jamesyox.svg4k.attr.attrs.y
import dev.jamesyox.svg4k.attr.attrs.y1
import dev.jamesyox.svg4k.attr.attrs.y2
import dev.jamesyox.svg4k.attr.types.obj.SvgColor
import dev.jamesyox.svg4k.attr.types.obj.none
import dev.jamesyox.svg4k.attr.types.obj.px
import dev.jamesyox.svg4k.consumers.svgString
import dev.jamesyox.svg4k.tags.categories.container.AllElementContainer
import dev.jamesyox.svg4k.tags.categories.container.ElementContainer
import dev.jamesyox.svg4k.tags.categories.container.unaryPlus
import dev.jamesyox.svg4k.tags.line
import dev.jamesyox.svg4k.tags.polygon
import dev.jamesyox.svg4k.tags.rect
import dev.jamesyox.svg4k.tags.svg
import dev.jamesyox.svg4k.tags.text
import dev.jamesyox.svg4k.util.translate
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.ceil
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
    val width = ceil(layout.boundingBox.maxX - layout.boundingBox.minX + 2 * padding).toInt()
    val height = ceil(layout.boundingBox.maxY - layout.boundingBox.minY + 2 * padding).toInt()

    return svgString(prettyPrint) {
        svg {
            viewBox = ViewBox(0, 0, width, height)
            rect {
                x = 0.none
                y = 0.none
                w = width.none
                h = height.none
                fill(theme.backgroundColor.svg)
            }

            group {
                transform {
                    translate(padding, padding)
                }

                val context = SvgRenderingContext { it() }
                context.drawBoard(layout, theme, middleLayer)
            }
        }
    }
}

class SvgRenderingContext(private val configure: (context(TagConsumer<*>, @SvgTagDSL Group) () -> Unit) -> Unit) : RenderingContext {
    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        configure {
            line {
                x1 = from.x.none
                y1 = from.y.none
                x2 = to.x.none
                y2 = to.y.none

                stroke(stroke.color.svg)
                strokeWidth = stroke.width.none

                strokeLinecap = StrokeLinecap.Round

                // TODO outline
            }
        }
    }

    override fun drawPolygon(shape: Polygon, color: Color?, outline: Stroke?) {
        configure {
            polygon {
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
        configure {
            text {
                fill(color.svg)
                fontWeight = FontWeight.Numeric(800)
                fontFamily = "system-ui, -apple-system, BlinkMacSystemFont, sans-serif"
                fs = FontSize.Value(fontSize.px)

                textAnchor = TextAnchor.Middle
                dominantBaseline = DominantBaseline.Middle

                x = listOf(point.x.none)
                y = listOf(point.y.none)

                +text
                // TODO outline
            }
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
