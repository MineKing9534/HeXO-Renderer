package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticMouseEvent
import de.mineking.hexo.api.formation.FormationRepository
import de.mineking.hexo.api.game.FinishedGameRepository
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.parse.parseRectilinearStateBKETurnNotation
import de.mineking.hexo.render.RectilinearNotationType
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
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.MouseEvent
import de.mineking.hexo.board.Board as HexoBoard

private const val DEFAULT_SIDEBAR_WIDTH = 384
private const val MIN_SIDEBAR_WIDTH = 320
private const val MAX_SIDEBAR_WIDTH = 560
private const val GITHUB_URL = "https://github.com/MineKing9534/HeXO-Renderer"

enum class BoardUpdateCause {
    NotationInput,
    Import,
}

@Composable
fun Sidebar(
    formationRepository: FormationRepository,
    finishedGameRepository: FinishedGameRepository,
    board: HexoBoard,
    onBoardChange: (BoardUpdateCause, HexoBoard) -> Unit,
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
        var parseError by remember { mutableStateOf<String?>(null) }
        var notation by remember { mutableStateOf("") }

        LaunchedEffect(board) {
            notation = board.renderRectilinearNotation(RectilinearNotationType.Compact)
            parseError = null
        }

        SidebarResizeHandle(resizing, ::startSidebarResize)
        SidebarHeader(parseError)
        NotationField(
            formationRepository = formationRepository,
            finishedGameRepository = finishedGameRepository,
            notation = notation,
            parseError = parseError,
            onChange = { cause, value ->
                notation = value
                if (value.isBlank()) {
                    onBoardChange(cause, HexoBoard())
                    return@NotationField
                }

                try {
                    onBoardChange(cause, value.parseRectilinearStateBKETurnNotation())
                    parseError = null
                } catch (e: HexoNotationException) {
                    parseError = e.message
                }
            },
        )
        SidebarNotationInfo(board, parseError)

        SidebarFooter()
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
    formationRepository: FormationRepository,
    finishedGameRepository: FinishedGameRepository,
    notation: String,
    parseError: String?,
    onChange: (BoardUpdateCause, String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var importDialogOpen by remember { mutableStateOf(false) }

    Div({ classes("space-y-2") }) {
        Div({ classes("flex", "items-center", "justify-between", "gap-3") }) {
            Div({ classes("text-sm", "font-semibold", "uppercase", "text-slate-400") }) {
                Text("Notation")
            }
            Button({
                classes(
                    "rounded-md", "border", "border-slate-700", "bg-slate-950", "px-2.5", "py-1",
                    "text-xs", "font-medium", "text-slate-300", "transition", "hover:bg-slate-800", "hover:text-slate-100",
                )
                onClick {
                    importDialogOpen = true
                }
            }) {
                Text("Import")
            }
        }

        TextArea {
            value(notation)
            placeholder("Board notation")
            onInput { onChange(BoardUpdateCause.NotationInput, it.value) }
            onFocus { focused = true }
            onBlur { focused = false }
            classes(
                "min-h-32", "w-full", "resize-y", "rounded-lg", "border-3", "p-3", "text-sm", "text-slate-100",
                "outline-none", "transition", "font-mono",
            )
            when {
                parseError != null -> classes("border-rose-400", "bg-slate-950")
                focused -> classes("border-emerald-400", "bg-slate-800")
                else -> classes("border-slate-700", "bg-slate-950")
            }
        }
    }

    if (importDialogOpen) {
        ImportDialog(
            formationRepository = formationRepository,
            finishedGameRepository = finishedGameRepository,
            onClose = { importDialogOpen = false },
            onConfirm = {
                importDialogOpen = false
                onChange(BoardUpdateCause.Import, it.renderRectilinearNotation(RectilinearNotationType.Compact))
            },
        )
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
        A(GITHUB_URL, {
            attr("target", "_blank")
            attr("rel", "noreferrer noopener")
            classes("text-slate-400", "transition", "hover:text-slate-200")
        }) {
            Text("GitHub")
        }
    }
}
