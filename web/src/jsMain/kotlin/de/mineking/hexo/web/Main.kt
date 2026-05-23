package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticMouseEvent
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.clone
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.parse.parseRectilinearStateBKETurnNotation
import de.mineking.hexo.render.RectilinearNotationType
import de.mineking.hexo.render.compose.Board
import de.mineking.hexo.render.compose.BoardViewport
import de.mineking.hexo.render.renderRectilinearNotation
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.MouseEvent
import de.mineking.hexo.board.Board as HexoBoard

private const val DEFAULT_SIDEBAR_WIDTH = 384
private const val MIN_SIDEBAR_WIDTH = 320
private const val MAX_SIDEBAR_WIDTH = 560
private const val GITHUB_URL = "https://github.com/MineKing9534/HeXO-Renderer"

fun main() {
    js("require('./style.css')")
    renderComposable(rootElementId = "root") {
        MainLayout()
    }
}

@Composable
private fun MainLayout() {
    var notation by remember { mutableStateOf("x") }
    var board by remember { mutableStateOf(notation.parseRectilinearStateBKETurnNotation()) }
    var parseError by remember { mutableStateOf<String?>(null) }

    fun updateNotation(value: String) {
        notation = value

        if (value.isBlank()) {
            board = HexoBoard()
            parseError = null
            return
        }

        try {
            board = value.parseRectilinearStateBKETurnNotation()
            parseError = null
        } catch (e: HexoNotationException) {
            parseError = e.message
        }
    }

    fun updateBoard(value: HexoBoard) {
        board = value
        notation = value.renderRectilinearNotation(RectilinearNotationType.Compact)
        parseError = null
    }

    Div({
        classes("flex", "h-screen", "w-screen", "overflow-hidden", "bg-slate-950", "font-sans", "text-slate-100")
    }) {
        BoardPane(board, onCellClick = {
            val updated = board.clone()
            updated[it].owner = CellOwner.X
            updateBoard(updated)
        })
        Sidebar(
            board = board,
            notation = notation,
            parseError = parseError,
            onNotationChange = ::updateNotation,
        )
    }
}

@Composable
private fun BoardPane(board: HexoBoard, onCellClick: ((CellCoordinate) -> Unit)? = null) {
    Div({ classes("min-w-0", "flex-1", "p-6") }) {
        Div({ classes("relative", "h-full", "overflow-hidden", "rounded-2xl", "border", "border-slate-800", "bg-slate-900", "shadow-2xl") }) {
            var viewport by remember { mutableStateOf(BoardViewport()) }

            Board(
                board = board,
                viewport = viewport,
                onViewportChange = { viewport = it },
                onCellClick = onCellClick,
                attrs = {
                    attr("width", "1200")
                    attr("height", "900")
                    classes("block", "h-full", "w-full")
                },
            )

            Button({
                classes(
                    "absolute", "bottom-3", "right-3", "z-10", "grid", "place-items-center", "rounded-lg", "py-1", "px-4", "border",
                    "border-slate-700", "bg-slate-950", "text-slate-300", "shadow-lg", "transition", "hover:bg-slate-800", "hover:text-slate-100",
                )
                onClick {
                    viewport = BoardViewport()
                }
            }) {
                Text("Reset View")
            }
        }
    }
}

@Composable
private fun SidebarResizeEffect(
    resizing: Boolean,
    onResize: (Int) -> Unit,
    onResizeEnd: () -> Unit,
) {
    DisposableEffect(resizing) {
        if (!resizing) return@DisposableEffect onDispose {}

        val mouseMove = EventListener { event ->
            event.preventDefault()
            val mouseEvent = event as MouseEvent
            onResize(
                (window.innerWidth - mouseEvent.clientX).coerceIn(
                    MIN_SIDEBAR_WIDTH,
                    MAX_SIDEBAR_WIDTH,
                ),
            )
        }
        val mouseUp = EventListener {
            onResizeEnd()
        }

        window.addEventListener("mousemove", mouseMove)
        window.addEventListener("mouseup", mouseUp)
        onDispose {
            window.removeEventListener("mousemove", mouseMove)
            window.removeEventListener("mouseup", mouseUp)
        }
    }
}

@Composable
private fun Sidebar(
    board: HexoBoard,
    notation: String,
    parseError: String?,
    onNotationChange: (String) -> Unit,
) {
    var width by remember { mutableStateOf(DEFAULT_SIDEBAR_WIDTH) }
    var resizing by remember { mutableStateOf(false) }

    SidebarResizeEffect(
        resizing = resizing,
        onResize = { width = it },
        onResizeEnd = {
            resizing = false
            document.body?.removeClass("select-none", "!cursor-col-resize", "[&_*]:!cursor-col-resize")
        },
    )

    fun startSidebarResize(event: SyntheticMouseEvent) {
        event.preventDefault()
        resizing = true
        document.body?.addClass("select-none", "!cursor-col-resize", "[&_*]:!cursor-col-resize")
    }

    Div({
        classes("relative", "flex", "shrink-0", "flex-col", "gap-8", "border-l", "border-slate-800", "bg-slate-900/80", "p-6", "shadow-2xl")
        attr("style", "width: ${width}px")
    }) {
        SidebarResizeHandle(resizing, ::startSidebarResize)
        SidebarHeader(parseError)
        NotationField(
            notation = notation,
            parseError = parseError,
            onChange = onNotationChange,
        )
        SidebarNotationInfo(board, parseError)

        SidebarFooter()
    }
}

@Composable
private fun SidebarResizeHandle(
    resizing: Boolean,
    onResizeStart: (SyntheticMouseEvent) -> Unit,
) {
    Div({
        onMouseDown(onResizeStart)
        classes("group", "absolute", "-left-1", "top-0", "grid", "h-full", "w-2", "cursor-col-resize", "place-items-center")
        if (resizing) {
            classes("bg-slate-700/30")
        } else {
            classes("hover:bg-slate-700/20")
        }
    }) {
        Div({
            classes("h-10", "w-1", "rounded-full", "bg-slate-600", "opacity-60", "transition", "group-hover:opacity-100")
        })
    }
}

@Composable
private fun SidebarHeader(parseError: String?) {
    Div({ classes("flex", "items-center", "justify-between") }) {
        Div({ classes("text-lg", "font-semibold") }) {
            Text("Board")
        }
        ParseStatus(parseError == null)
    }
}

@Composable
private fun ParseStatus(valid: Boolean) {
    Span({
        classes("rounded-full", "px-2.5", "py-1", "text-xs", "font-medium", "ring-1")
        if (valid) {
            classes("bg-emerald-500/15", "ring-emerald-400/30", "text-emerald-400")
        } else {
            classes("bg-rose-500/15", "ring-rose-400/30", "text-rose-400")
        }
    }) {
        Text(if (valid) "Parsed" else "Invalid")
    }
}

@Composable
private fun NotationField(
    notation: String,
    parseError: String?,
    onChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Div({ classes("space-y-2") }) {
        Div({ classes("text-xs", "font-semibold", "uppercase", "text-slate-400") }) {
            Text("Notation")
        }

        TextArea {
            value(notation)
            placeholder("Board notation")
            onInput { onChange(it.value) }
            onFocus { focused = true }
            onBlur { focused = false }
            classes("min-h-32", "w-full", "resize-y", "rounded-lg", "border-3", "p-3", "text-sm", "text-slate-100", "outline-none", "transition")
            when {
                parseError != null -> classes("border-rose-400", "bg-slate-950")
                focused -> classes("border-emerald-400", "bg-slate-800")
                else -> classes("border-slate-700", "bg-slate-950")
            }
        }
    }
}

@Composable
private fun SidebarNotationInfo(board: HexoBoard, parseError: String?) {
    Div({
        classes("min-h-5", "text-sm", "leading-relaxed")
        if (parseError == null) {
            classes("text-slate-500")
        } else {
            classes("text-rose-400")
        }
    }) {
        Text(parseError ?: "${board.cells.size} cells")
    }
}

@Composable
private fun SidebarFooter() {
    Div({
        classes(
            "mt-auto", "flex", "items-center", "gap-2", "justify-between", "border-t", "border-slate-800", "pt-4", "px-2",
            "text-xs", "text-slate-500",
        )
    }) {
        Span {
            Text("Copyright © 2026 MineKing")
        }
        A(
            attrs = {
                attr("href", GITHUB_URL)
                attr("target", "_blank")
                attr("rel", "noreferrer noopener")
                classes("text-slate-400", "transition", "hover:text-slate-200")
            },
        ) {
            Text("GitHub")
        }
    }
}
