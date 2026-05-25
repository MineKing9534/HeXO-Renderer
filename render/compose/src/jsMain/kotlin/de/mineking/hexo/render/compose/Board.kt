package de.mineking.hexo.render.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.render.image.BoardRenderBounds
import de.mineking.hexo.render.image.BoardRenderLayout
import de.mineking.hexo.render.image.Color
import de.mineking.hexo.render.image.Stroke
import de.mineking.hexo.render.image.center
import de.mineking.hexo.render.image.createRenderLayout
import de.mineking.hexo.render.image.div
import de.mineking.hexo.render.image.drawBoard
import de.mineking.hexo.render.image.plus
import de.mineking.hexo.render.image.topLeft
import de.mineking.hexo.render.image.withAlpha
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.ContentBuilder
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent

private const val BOARD_LAYOUT_RADIUS = 255.0
private val cellHoverColor = Color.rgb(0x7dd3fc)

@Composable
fun Board(
    board: Board,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    onCellClick: (MouseEvent.(CellCoordinate) -> Unit)? = null,
    onBoardRightClick: (MouseEvent.(BoardRightClickEvent) -> Unit)? = null,
    content: ContentBuilder<HTMLCanvasElement>? = null,
) {
    var viewport by remember { mutableStateOf<BoardViewport?>(null) }
    Board(board, viewport, { viewport = it }, attrs, onCellClick, onBoardRightClick, content)
}

@Composable
fun Board(
    board: Board,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport) -> Unit,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    onCellClick: (MouseEvent.(CellCoordinate) -> Unit)? = null,
    onBoardRightClick: (MouseEvent.(BoardRightClickEvent) -> Unit)? = null,
    content: ContentBuilder<HTMLCanvasElement>? = null,
) {
    var element by remember { mutableStateOf<HTMLCanvasElement?>(null) }
    var dragging by remember { mutableStateOf(false) }
    var hoveredCell by remember { mutableStateOf<CellCoordinate?>(null) }

    val zoom = viewport?.zoom ?: 0.2
    val layout = remember(board, zoom) { board.createRenderLayout(BOARD_LAYOUT_RADIUS * zoom, BoardRenderBounds.IncludeSurroundings) }
    val effectiveViewport = viewport ?: BoardViewport(zoom = zoom, center = layout.boundingBox.center / zoom)

    fun redraw() {
        element?.drawBoard(layout, effectiveViewport, hoveredCell)
    }

    ResizeHandler(element) { redraw() }
    LaunchedEffect(effectiveViewport, layout, hoveredCell) { redraw() }

    BoardInteractions(
        element = element,
        renderLayout = { layout },
        viewport = { effectiveViewport },
        onViewportChange = onViewportChange,
        onDraggingChange = { dragging = it },
        onCellHoverChange = { hoveredCell = it },
        onCellClick = { onCellClick?.invoke(this, it) },
        onBoardRightClick = { onBoardRightClick?.invoke(this, it) },
    )

    Canvas({
        if (attrs != null) attrs()
        style {
            cursor(if (dragging) "grabbing" else "grab")
        }
        ref {
            element = it
            onDispose {}
        }
    }, content)
}

@Composable
private fun ResizeHandler(element: HTMLCanvasElement?, onResize: () -> Unit) {
    val onResize by rememberUpdatedState(onResize)

    DisposableEffect(element) {
        val canvas = element ?: return@DisposableEffect onDispose {}
        val observer = ResizeObserver { onResize() }

        observer.observe(canvas)
        onDispose {
            observer.disconnect()
        }
    }
}

private external class ResizeObserver(@Suppress("unused") callback: () -> Unit) {
    fun observe(target: Element)
    fun disconnect()
}

private fun HTMLCanvasElement.drawBoard(
    layout: BoardRenderLayout,
    viewport: BoardViewport,
    hoveredCell: CellCoordinate?,
) {
    width = clientWidth
    height = clientHeight

    drawBoard(
        layout = layout,
        padding = BOARD_RENDER_PADDING,
        offset = viewport.offset(this) + layout.boundingBox.topLeft,
    ) {
        if (hoveredCell == null) return@drawBoard
        val cell = layout.board.cells[hoveredCell]

        renderingContext.drawPolygon(
            shape = hoveredCell.createHex(),
            color = cellHoverColor.withAlpha(48),
            outline = if (cell != null && (cell.highlight != null || cell.focused)) null else Stroke(cellHoverColor.withAlpha(140), 2f),
        )
    }
}
