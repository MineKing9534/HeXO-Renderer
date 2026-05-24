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
import de.mineking.hexo.render.image.BoardRenderLayout
import de.mineking.hexo.render.image.center
import de.mineking.hexo.render.image.createRenderLayout
import de.mineking.hexo.render.image.div
import de.mineking.hexo.render.image.drawBoard
import de.mineking.hexo.render.image.plus
import de.mineking.hexo.render.image.topLeft
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.ContentBuilder
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement

private const val BOARD_LAYOUT_RADIUS = 255.0

@Composable
fun Board(
    board: Board,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    onCellClick: ((CellCoordinate) -> Unit)? = null,
    content: ContentBuilder<HTMLCanvasElement>? = null,
) {
    var viewport by remember { mutableStateOf<BoardViewport?>(null) }
    Board(board, viewport, { viewport = it }, attrs, onCellClick, content)
}

@Composable
fun Board(
    board: Board,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport) -> Unit,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    onCellClick: ((CellCoordinate) -> Unit)? = null,
    content: ContentBuilder<HTMLCanvasElement>? = null,
) {
    var element by remember { mutableStateOf<HTMLCanvasElement?>(null) }
    var dragging by remember { mutableStateOf(false) }

    val zoom = viewport?.zoom ?: 0.2
    val layout = remember(board, zoom) { board.createRenderLayout(BOARD_LAYOUT_RADIUS * zoom) }
    val effectiveViewport = viewport ?: BoardViewport(zoom = zoom, center = layout.boundingBox.center / zoom)

    fun redraw() {
        element?.drawBoard(board, effectiveViewport, layout)
    }

    ResizeHandler(element) { redraw() }
    LaunchedEffect(board, effectiveViewport, layout) { redraw() }

    BoardInteractions(
        element = element,
        renderLayout = { layout },
        viewport = { effectiveViewport },
        onViewportChange = onViewportChange,
        onDraggingChange = { dragging = it },
        onCellClick = { onCellClick?.invoke(it) },
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

private fun HTMLCanvasElement.drawBoard(board: Board, viewport: BoardViewport, layout: BoardRenderLayout) {
    width = clientWidth
    height = clientHeight

    drawBoard(
        board = board,
        layoutRadius = BOARD_LAYOUT_RADIUS * viewport.zoom,
        padding = BOARD_RENDER_PADDING,
        offset = viewport.offset(this) + layout.boundingBox.topLeft,
    )
}
