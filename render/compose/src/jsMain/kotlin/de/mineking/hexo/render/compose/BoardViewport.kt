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
import org.w3c.dom.events.MouseEventInit
import org.w3c.dom.events.WheelEvent
import kotlin.math.sqrt
import kotlin.reflect.KProperty

internal const val BOARD_RENDER_PADDING = 128

private const val MIN_ZOOM = 0.07
private const val MAX_ZOOM = 0.8
private const val ZOOM_STEP = 1.1
private const val PINCH_ZOOM_DEAD_ZONE = 0.025
private const val LONG_PRESS_DELAY = 450
private const val LONG_PRESS_MOVE_TOLERANCE = 12.0

private const val PRIMARY_BUTTON = 0
private const val SECONDARY_BUTTON = 2

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

data class BoardRightClickEvent(val from: CellCoordinate, val to: CellCoordinate, val final: Boolean)

@Composable
internal fun BoardInteractions(
    element: HTMLCanvasElement?,
    renderLayout: () -> BoardRenderLayout,
    viewport: () -> BoardViewport,
    onViewportChange: (BoardViewport) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onCellHoverChange: (CellCoordinate?) -> Unit,
    onCellClick: MouseEvent.(CellCoordinate) -> Unit,
    onBoardRightClick: MouseEvent.(BoardRightClickEvent) -> Unit,
) {
    val onViewportChange by rememberUpdatedState(onViewportChange)
    val onDraggingChange by rememberUpdatedState(onDraggingChange)
    val onCellHoverChange by rememberUpdatedState(onCellHoverChange)
    val onCellClick by rememberUpdatedState(onCellClick)
    val onBoardRightClick by rememberUpdatedState(onBoardRightClick)
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
            onCellHoverChange = { onCellHoverChange(it) },
            onCellClick = { onCellClick(it) },
            onBoardRightClick = { onBoardRightClick(it) },
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
    private val onCellHoverChange: (CellCoordinate?) -> Unit,
    private val onCellClick: MouseEvent.(CellCoordinate) -> Unit,
    private val onBoardRightClick: MouseEvent.(BoardRightClickEvent) -> Unit,
) {
    private val renderLayout by renderLayout
    private var viewport by viewport.withSetter(onViewportChange)

    private var rightClickStart: MouseEvent? = null
    private var lastRightClickEnd: CellCoordinate? = null
    private var lastDragPosition: Point? = null
        set(value) {
            val wasDragging = field != null
            val isDragging = value != null
            if (wasDragging != isDragging) onDraggingChange(isDragging)

            field = value
        }

    private var touchDragPosition: Point? = null
    private var touchPinchGesture: PinchGesture? = null
    private var touchLongPress: LongPress? = null
    private var touchMoved = false

    private var suppressNextClick = false
    private var hoveredCell: CellCoordinate? = null
        set(value) {
            if (field == value) return
            field = value
            onCellHoverChange(value)
        }

    private fun pointerDown(event: Event) {
        require(event is MouseEvent)
        if (event.isTouchPointer()) return

        event.preventDefault()

        hoveredCell = null

        when (event.button.toInt()) {
            PRIMARY_BUTTON -> {
                suppressNextClick = false
                beginDrag(event)
            }

            SECONDARY_BUTTON -> {
                rightClickStart = event
                lastRightClickEnd = null
            }
        }
    }

    private fun pointerMove(event: Event) {
        require(event is MouseEvent)
        if (event.isTouchPointer()) return

        event.preventDefault()

        when {
            lastDragPosition != null -> dragMouseTo(event.position())
            rightClickStart != null -> triggerRightClick(event.position(), final = false)
            else -> hoveredCell = cellAt(event.position())
        }
    }

    private fun pointerUp(event: Event) {
        require(event is MouseEvent)
        if (event.isTouchPointer()) return

        if (event.button.toInt() == SECONDARY_BUTTON) {
            triggerRightClick(event.position(), final = true)
        }

        finishDrag(event)
        hoveredCell = cellAt(event.position())
    }

    private fun touchStart(event: Event) {
        val touches = event.touches()
        if (touches.isEmpty()) return

        suppressNextClick = false
        touchMoved = false
        hoveredCell = null
        rightClickStart = null
        lastDragPosition = null

        if (touches.size >= 2) {
            event.preventDefault()
            cancelLongPress()
            beginTouchPinch(touches)
        } else {
            touchPinchGesture = null
            touchDragPosition = touches.first()
            scheduleLongPress(touches.first())
        }
    }

    private fun touchMove(event: Event) {
        val touches = event.touches()
        if (touches.isEmpty()) return

        if (touches.size >= 2) {
            event.preventDefault()
            cancelLongPress()
            if (touchPinchGesture == null) beginTouchPinch(touches)
            pinchTo(touches)
        } else {
            touchPinchGesture = null
            if (moveSingleTouchTo(touches.first())) {
                event.preventDefault()
            }
        }
    }

    private fun touchEnd(event: Event) {
        val touches = event.touches()

        val longPress = touchLongPress
        if (longPress?.active == true) {
            triggerLongPressRightClick(longPress, longPress.lastPosition, final = true)
        }

        cancelLongPress()
        touchPinchGesture = null
        touchDragPosition = touches.singleOrNull()
        if (touches.isEmpty() && touchMoved) suppressNextClick = true
    }

    private fun click(event: Event) {
        val mouseEvent = event as MouseEvent
        if (suppressNextClick) {
            suppressNextClick = false
            return
        }

        event.onCellClick(cellAt(mouseEvent.position()))
    }

    private fun wheel(event: Event) {
        val wheelEvent = event as WheelEvent
        wheelEvent.preventDefault()
        zoomAt(wheelEvent)
    }

    private fun suppressContextMenu(event: Event) {
        event.preventDefault()
    }

    private fun clearHover(@Suppress("unused") event: Event) {
        hoveredCell = null
    }

    fun attach() {
        canvas.addEventListener("pointerdown", ::pointerDown)
        canvas.addEventListener("pointermove", ::pointerMove)
        canvas.addEventListener("pointerup", ::pointerUp)
        canvas.addEventListener("pointercancel", ::finishDrag)
        canvas.addEventListener("touchstart", ::touchStart)
        canvas.addEventListener("touchmove", ::touchMove)
        canvas.addEventListener("touchend", ::touchEnd)
        canvas.addEventListener("touchcancel", ::finishTouch)
        canvas.addEventListener("click", ::click)
        canvas.addEventListener("wheel", ::wheel)
        canvas.addEventListener("contextmenu", ::suppressContextMenu)
        canvas.addEventListener("pointerleave", ::clearHover)
        window.addEventListener("blur", ::finishDrag)
        window.addEventListener("blur", ::finishTouch)
        window.addEventListener("blur", ::clearHover)
    }

    fun detach() {
        canvas.removeEventListener("pointerdown", ::pointerDown)
        canvas.removeEventListener("pointermove", ::pointerMove)
        canvas.removeEventListener("pointerup", ::pointerUp)
        canvas.removeEventListener("pointercancel", ::finishDrag)
        canvas.removeEventListener("touchstart", ::touchStart)
        canvas.removeEventListener("touchmove", ::touchMove)
        canvas.removeEventListener("touchend", ::touchEnd)
        canvas.removeEventListener("touchcancel", ::finishTouch)
        canvas.removeEventListener("click", ::click)
        canvas.removeEventListener("wheel", ::wheel)
        canvas.removeEventListener("contextmenu", ::suppressContextMenu)
        canvas.removeEventListener("pointerleave", ::clearHover)
        window.removeEventListener("blur", ::finishDrag)
        window.removeEventListener("blur", ::finishTouch)
        window.removeEventListener("blur", ::clearHover)
        lastDragPosition = null
        finishTouch()
        hoveredCell = null
    }

    private fun triggerRightClick(to: Point, final: Boolean) {
        val start = rightClickStart ?: return

        val end = cellAt(to)
        if (end == lastRightClickEnd && !final) return

        start.onBoardRightClick(BoardRightClickEvent(cellAt(start.position()), end, final = final))

        if (final) {
            rightClickStart = null
            lastRightClickEnd = null
        } else {
            lastRightClickEnd = end
        }
    }

    private fun dragMouseTo(position: Point) {
        val lastPosition = lastDragPosition ?: return
        viewport = viewport.pan(position - lastPosition)
        suppressNextClick = true
        lastDragPosition = position
    }

    private fun dragTouchTo(position: Point): Boolean {
        val lastPosition = touchDragPosition ?: run {
            touchDragPosition = position
            return false
        }

        viewport = viewport.pan(position - lastPosition)
        suppressNextClick = true
        touchMoved = true
        touchDragPosition = position
        return true
    }

    private fun moveSingleTouchTo(position: Point): Boolean {
        val longPress = touchLongPress ?: return dragTouchTo(position)
        longPress.lastPosition = position

        return when {
            longPress.active -> {
                triggerLongPressRightClick(longPress, position, final = false)
                true
            }

            longPress.startPosition.distanceTo(position) > LONG_PRESS_MOVE_TOLERANCE -> {
                cancelLongPress()
                dragTouchTo(position)
            }

            else -> false
        }
    }

    private fun scheduleLongPress(position: Point) {
        cancelLongPress()

        val timeout = window.setTimeout({
            val longPress = touchLongPress ?: return@setTimeout
            longPress.active = true
            suppressNextClick = true
            touchMoved = true
            touchDragPosition = null
            triggerLongPressRightClick(longPress, longPress.lastPosition, final = false)
        }, LONG_PRESS_DELAY)

        touchLongPress = LongPress(
            startPosition = position,
            lastPosition = position,
            timeout = timeout,
        )
    }

    private fun cancelLongPress() {
        val longPress = touchLongPress ?: return
        window.clearTimeout(longPress.timeout)
        touchLongPress = null
    }

    private fun triggerLongPressRightClick(longPress: LongPress, position: Point, final: Boolean) {
        val end = cellAt(position)
        if (end == longPress.lastCell && !final) return

        MouseEvent("contextmenu", MouseEventInit()).onBoardRightClick(
            BoardRightClickEvent(
                from = cellAt(longPress.startPosition),
                to = end,
                final = final,
            ),
        )
        longPress.lastCell = end
    }

    private fun beginTouchPinch(touches: List<Point>) {
        touchDragPosition = null
        touchPinchGesture = touchPinch(touches)?.let { PinchGesture(start = it, viewport = viewport) }
        suppressNextClick = true
    }

    private fun pinchTo(touches: List<Point>) {
        val gesture = touchPinchGesture ?: touchPinch(touches)
            ?.let { PinchGesture(start = it, viewport = viewport) }
            ?.also { touchPinchGesture = it }
            ?: return

        val current = touchPinch(touches) ?: return

        val rect = canvas.getBoundingClientRect()
        val scale = if (gesture.start.distance == 0.0) {
            1.0
        } else {
            (current.distance / gesture.start.distance).let {
                if (it in (1 - PINCH_ZOOM_DEAD_ZONE)..(1 + PINCH_ZOOM_DEAD_ZONE)) 1.0 else it
            }
        }

        viewport = gesture.viewport
            .zoomAt(
                value = gesture.viewport.zoom * scale,
                anchor = gesture.start.center - Point(rect.left, rect.top),
                canvas = canvas,
            )
            .pan(current.center - gesture.start.center)

        suppressNextClick = true
        touchMoved = true
    }

    private fun touchPinch(touches: List<Point>): Pinch? {
        if (touches.size < 2) return null

        val delta = touches[0] - touches[1]
        return Pinch(
            center = (touches[0] + touches[1]) / 2.0,
            distance = sqrt(delta.x * delta.x + delta.y * delta.y),
        )
    }

    private fun beginDrag(event: MouseEvent) {
        lastDragPosition = event.position()
        capturePointer(event)
    }

    private fun capturePointer(event: Event) {
        val pointerId = event.asDynamic().pointerId ?: return
        canvas.setPointerCapture(pointerId)
    }

    private fun finishDrag(event: Event) {
        lastDragPosition = null
        releasePointer(event)
    }

    private fun finishTouch(@Suppress("unused") event: Event? = null) {
        cancelLongPress()
        touchDragPosition = null
        touchPinchGesture = null
        touchMoved = false
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
        return renderLayout.run { point.toCoordinate() }
    }

    private fun MouseEvent.position() = Point(clientX.toDouble(), clientY.toDouble())

    private fun Event.isTouchPointer() = asDynamic().pointerType == "touch"

    private fun Event.touches(): List<Point> {
        val touches = asDynamic().touches
        val length = (touches.length as Number).toInt()

        return List(length) {
            val touch = touches.item(it)
            Point(
                (touch.clientX as Number).toDouble(),
                (touch.clientY as Number).toDouble(),
            )
        }
    }

    private data class Pinch(val center: Point, val distance: Double)
    private data class PinchGesture(val start: Pinch, val viewport: BoardViewport)
    private data class LongPress(
        val startPosition: Point,
        var lastPosition: Point,
        val timeout: Int,
        var active: Boolean = false,
        var lastCell: CellCoordinate? = null,
    )
}

private fun Point.distanceTo(point: Point): Double {
    val delta = this - point
    return sqrt(delta.x * delta.x + delta.y * delta.y)
}
