package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import de.mineking.hexo.render.renderRectilinearStateBKETurnNotation
import de.mineking.hexo.web.components.Dialog
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.readOnly
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.url.URL
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
    placementMode: MutableState<CellPlacementMode>,
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
        classes(
            "relative", "flex", "max-h-[30dvh]", "w-full", "shrink-0", "flex-col", "gap-5", "overflow-y-auto", "border-t", "border-slate-800",
            "bg-slate-900/80", "p-4", "shadow-2xl", "md:max-h-none", "md:w-[var(--sidebar-width)]", "md:gap-8", "md:border-l", "md:border-t-0",
            "md:p-6",
        )
        attr("style", "--sidebar-width: ${width}px")
    }) {
        var parseError by remember { mutableStateOf<String?>(null) }
        var notation by remember { mutableStateOf("") }

        LaunchedEffect(board) {
            notation = board.renderRectilinearStateBKETurnNotation(RectilinearNotationType.Compact)
            parseError = null
        }

        SidebarResizeHandle(resizing, ::startSidebarResize)
        SidebarHeader(parseError)

        Div({ classes("flex", "flex-col", "gap-2") }) {
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
        }

        PlacementMode(placementMode)

        SidebarFooter()
    }
}

@Composable
private fun PlacementMode(placementMode: MutableState<CellPlacementMode>) {
    var placementMode by placementMode
    Div({ classes("space-y-2") }) {
        Div({ classes("text-sm", "font-semibold", "uppercase", "text-slate-400") }) {
            Text("Placement Mode")
        }

        Div({ classes("flex", "gap-3") }) {
            CellPlacementMode.entries.forEach { mode ->
                Button({
                    classes(
                        "rounded-full", "px-8", "py-1.25", "m-px", "text-xs", "font-medium", "ring-1", "backdrop-blur-sm", "transition-all",
                        "duration-200", "hover:shadow-lg",
                    )

                    if (mode == placementMode) {
                        classes(
                            "bg-amber-400/12", "ring-amber-200/40", "text-amber-300", "shadow-amber-950/30",
                            "hover:bg-amber-400/18", "hover:ring-amber-200/60", "hover:text-amber-200",
                        )
                    } else {
                        classes(
                            "bg-slate-800/40", "ring-slate-600/60", "text-slate-300",
                            "hover:bg-slate-700/50", "hover:ring-slate-500/60", "hover:text-slate-100",
                        )
                    }

                    onClick { placementMode = mode }
                }) {
                    Text(mode.name)
                }
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
private fun SidebarResizeHandle(
    resizing: Boolean,
    onResizeStart: (SyntheticMouseEvent) -> Unit,
) {
    Div({
        onMouseDown(onResizeStart)
        classes("group", "absolute", "-left-1", "top-0", "hidden", "h-full", "w-2", "cursor-col-resize", "place-items-center", "md:grid")
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
        classes("rounded-full", "px-3", "py-1", "m-px", "text-xs", "font-medium", "ring-1")
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

    Div({ classes("space-y-2") }) {
        Div({ classes("flex", "items-center", "justify-between", "gap-3") }) {
            Div({ classes("text-sm", "font-semibold", "uppercase", "text-slate-400") }) {
                Text("Notation")
            }
            NotationActions(formationRepository, finishedGameRepository, notation, onChange)
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
}

@Composable
private fun NotationActions(
    formationRepository: FormationRepository,
    finishedGameRepository: FinishedGameRepository,
    notation: String,
    onChange: (BoardUpdateCause, String) -> Unit,
) {
    @Composable
    fun Button(label: String, enabled: Boolean = true, onClick: () -> Unit) {
        Button({
            if (!enabled) disabled()
            classes(
                "rounded-md", "border", "border-slate-700", "bg-slate-950", "px-2.5", "py-1", "text-nowrap",
                "text-xs", "font-medium", "text-slate-300", "disabled:text-slate-400", "transition",
                "not-disabled:hover:bg-slate-800", "not-disabled:hover:text-slate-100",
            )
            onClick { onClick() }
        }) {
            Text(label)
        }
    }

    var importDialogOpen by remember { mutableStateOf(false) }
    Div({ classes("flex", "gap-2") }) {
        var link by remember { mutableStateOf<String?>(null) }
        Button("Copy Link", enabled = notation.isNotBlank()) {
            val url = URL(window.location.href)
            url.searchParams.set("position", notation.replace("/", "_"))
            link = url.toString()
        }

        if (link != null) {
            Dialog(
                title = "Position Link",
                onClose = { link = null },
            ) {
                Input(InputType.Url) {
                    value(link ?: "")
                    classes(
                        "w-full", "resize-y", "rounded-lg", "border-3", "p-3", "text-sm", "text-slate-100",
                        "outline-none", "transition", "font-mono", "border-slate-700", "bg-slate-950",
                        "focus:border-emerald-400", "focus:bg-slate-800", "text-ellipsis",
                    )
                    readOnly()
                }
            }
        }

        Button("Import Position") { importDialogOpen = true }
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
        Text(parseError ?: (if (board.cells.size == 1) "1 cell" else "${board.cells.size} cells"))
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
