package de.mineking.hexo.render.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.render.image.BoardRenderLayout
import de.mineking.hexo.render.image.Point
import de.mineking.hexo.render.image.div
import de.mineking.hexo.render.image.minus
import de.mineking.hexo.render.image.plus
import de.mineking.hexo.render.image.times
import de.mineking.hexo.render.image.topLeft
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import kotlin.reflect.KProperty

internal const val BOARD_RENDER_PADDING = 128

private const val MIN_ZOOM = 0.07
private const val MAX_ZOOM = 0.8
private const val PRIMARY_BUTTON = 0
private const val PRIMARY_BUTTON_MASK = 1
private const val ZOOM_STEP = 1.1

data class BoardViewport(val zoom: Double, val center: Point) {
    internal fun pan(delta: Point) = copy(center = center - delta / zoom)

    internal fun zoomAt(value: Double, anchor: Point, canvas: HTMLCanvasElement): BoardViewport {
        val newZoom = value.coerceIn(MIN_ZOOM, MAX_ZOOM)

        val anchorOffset = anchor - Point(canvas.clientWidth / 2.0, canvas.clientHeight / 2.0)
        return copy(
            zoom = newZoom,
            center = center + (anchorOffset / zoom) - (anchorOffset / newZoom),
        )
    }

    internal fun offset(canvas: HTMLCanvasElement) = Point(
        canvas.clientWidth / 2.0 - BOARD_RENDER_PADDING,
        canvas.clientHeight / 2.0 - BOARD_RENDER_PADDING,
    ) - center * zoom
}

@Composable
internal fun BoardInteractions(
    element: HTMLCanvasElement?,
    renderLayout: () -> BoardRenderLayout,
    viewport: () -> BoardViewport,
    onViewportChange: (BoardViewport) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onCellClick: (CellCoordinate) -> Unit,
) {
    val onViewportChange by rememberUpdatedState(onViewportChange)
    val onDraggingChange by rememberUpdatedState(onDraggingChange)
    val onCellClick by rememberUpdatedState(onCellClick)
    val currentRenderLayout by rememberUpdatedState(renderLayout)
    val currentViewport by rememberUpdatedState(viewport)

    DisposableEffect(element) {
        val canvas = element ?: return@DisposableEffect onDispose {}
        val listeners = BoardEventListeners(
            canvas = canvas,
            renderLayout = { currentRenderLayout() },
            viewport = { currentViewport() },
            onViewportChange = { onViewportChange(it) },
            onDraggingChange = { onDraggingChange(it) },
            onCellClick = { onCellClick(it) },
        )

        listeners.attach()
        onDispose { listeners.detach() }
    }
}

private operator fun <T> (() -> T).getValue(thisRef: Any?, property: KProperty<*>) = this()
private data class MutableFunctionDelegate<T>(val get: () -> T, val set: (T) -> Unit) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = get()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
}

private fun <T> (() -> T).withSetter(set: (T) -> Unit) = MutableFunctionDelegate(this, set)

private class BoardEventListeners(
    private val canvas: HTMLCanvasElement,
    renderLayout: () -> BoardRenderLayout,
    viewport: () -> BoardViewport,
    onViewportChange: (BoardViewport) -> Unit,
    private val onDraggingChange: (Boolean) -> Unit,
    private val onCellClick: (CellCoordinate) -> Unit,
) {
    private val renderLayout by renderLayout
    private var viewport by viewport.withSetter(onViewportChange)

    private var dragPosition: Point? = null
    private var suppressNextClick = false

    private fun pointerDown(event: Event) {
        val mouseEvent = event as MouseEvent
        if (mouseEvent.button.toInt() != PRIMARY_BUTTON) return

        mouseEvent.preventDefault()
        suppressNextClick = false

        beginDrag(event)
    }

    private fun pointerMove(event: Event) {
        val mouseEvent = event as MouseEvent
        if (dragPosition == null) return

        if (!mouseEvent.primaryButtonPressed()) {
            finishDrag(event)
            return
        }

        mouseEvent.preventDefault()
        dragTo(mouseEvent.position())
    }

    private fun endDrag(event: Event) {
        finishDrag(event)
    }

    private fun click(event: Event) {
        val mouseEvent = event as MouseEvent
        if (suppressNextClick) {
            suppressNextClick = false
            return
        }

        onCellClick(cellAt(mouseEvent.position()))
    }

    private fun wheel(event: Event) {
        val wheelEvent = event as WheelEvent
        wheelEvent.preventDefault()
        zoomAt(wheelEvent)
    }

    fun attach() {
        canvas.addEventListener("pointerdown", ::pointerDown)
        canvas.addEventListener("pointermove", ::pointerMove)
        canvas.addEventListener("pointerup", ::endDrag)
        canvas.addEventListener("pointercancel", ::endDrag)
        canvas.addEventListener("click", ::click)
        canvas.addEventListener("wheel", ::wheel)
        window.addEventListener("blur", ::endDrag)
    }

    fun detach() {
        canvas.removeEventListener("pointerdown", ::pointerDown)
        canvas.removeEventListener("pointermove", ::pointerMove)
        canvas.removeEventListener("pointerup", ::endDrag)
        canvas.removeEventListener("pointercancel", ::endDrag)
        canvas.removeEventListener("click", ::click)
        canvas.removeEventListener("wheel", ::wheel)
        window.removeEventListener("blur", ::endDrag)
        onDraggingChange(false)
    }

    private fun dragTo(position: Point) {
        val lastPosition = dragPosition ?: return

        viewport = viewport.pan(position - lastPosition)

        suppressNextClick = true
        dragPosition = position
    }

    private fun beginDrag(event: MouseEvent) {
        dragPosition = event.position()
        onDraggingChange(true)
        capturePointer(event)
    }

    private fun capturePointer(event: Event) {
        val pointerId = event.asDynamic().pointerId ?: return
        canvas.setPointerCapture(pointerId)
    }

    private fun finishDrag(event: Event) {
        dragPosition = null
        onDraggingChange(false)
        releasePointer(event)
    }

    private fun releasePointer(event: Event) {
        val pointerId = event.asDynamic().pointerId ?: return
        if (canvas.hasPointerCapture(pointerId)) {
            canvas.releasePointerCapture(pointerId)
        }
    }

    private fun zoomAt(event: WheelEvent) {
        val position = event.position()
        val rect = canvas.getBoundingClientRect()

        viewport = viewport.zoomAt(
            value = viewport.zoom * if (event.deltaY < 0) ZOOM_STEP else 1 / ZOOM_STEP,
            anchor = position - Point(rect.left, rect.top),
            canvas = canvas,
        )
    }

    private fun cellAt(position: Point): CellCoordinate {
        val rect = canvas.getBoundingClientRect()

        val canvasTopLeft = Point(rect.left + BOARD_RENDER_PADDING, rect.top + BOARD_RENDER_PADDING)
        val point = position - canvasTopLeft - viewport.offset(canvas) - renderLayout.boundingBox.topLeft
        return renderLayout.toCoordinate(point)
    }

    private fun MouseEvent.position() = Point(clientX.toDouble(), clientY.toDouble())
    private fun MouseEvent.primaryButtonPressed() = (buttons.toInt() and PRIMARY_BUTTON_MASK) != 0
}
