package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.createRepositories
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.clone
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.parse.parseRectilinearStateBKETurnNotation
import de.mineking.hexo.render.RectilinearNotationType
import de.mineking.hexo.render.compose.Board
import de.mineking.hexo.render.compose.BoardViewport
import de.mineking.hexo.render.renderRectilinearNotation
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLDivElement
import de.mineking.hexo.board.Board as HexoBoard

const val URL = "https://hexo.did.science"
private const val PROXY = "https://hexo.mineking.dev/proxy"

fun main() {
    js("require('./style.css')")
    renderComposable(rootElementId = "root") {
        val client = remember { HexoApiClient(host = PROXY, socketIOOptions = null) }
        MainLayout(client)
    }
}

@Composable
private fun MainLayout(client: HexoApiClient) {
    val repositories = remember(client) { client.createRepositories() }

    var notation by remember { mutableStateOf("x") }
    var viewport by remember { mutableStateOf<BoardViewport?>(null) }

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
        BoardPane(board, viewport, onViewportChange = { viewport = it }, onCellClick = {
            val updated = board.clone()
            updated[it].owner = CellOwner.X
            updateBoard(updated)
        })
        Sidebar(
            formationRepository = repositories.formations,
            finishedGameRepository = repositories.finishedGames,
            board = board,
            notation = notation,
            parseError = parseError,
            onNotationChange = { cause, notation ->
                updateNotation(notation)
                if (cause == NotationUpdateCause.Import) {
                    viewport = null
                }
            },
        )
    }
}

@Composable
private fun BoardPane(
    board: HexoBoard,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport?) -> Unit,
    onCellClick: ((CellCoordinate) -> Unit)? = null,
) {
    Div({ classes("min-w-0", "flex-1", "p-6") }) {
        Div({ classes("relative", "h-full", "overflow-hidden", "rounded-2xl", "border", "border-slate-800", "bg-slate-900", "shadow-2xl") }) {
            Board(
                board = board,
                viewport = viewport,
                onViewportChange = onViewportChange,
                onCellClick = onCellClick,
                attrs = {
                    attr("width", "1200")
                    attr("height", "900")
                    classes("block", "h-full", "w-full")
                },
            )

            @Composable
            fun Edge(attrs: AttrBuilderContext<HTMLDivElement>? = null) {
                Div({
                    classes("pointer-events-none", "absolute", "z-10", "from-slate-900", "via-transparent", "to-transparent")
                    attrs?.invoke(this)
                })
            }

            Edge { classes("inset-x-0", "top-0", "h-4", "bg-linear-to-b") }
            Edge { classes("inset-x-0", "bottom-0", "h-4", "bg-linear-to-t") }
            Edge { classes("inset-y-0", "left-0", "w-4", "bg-linear-to-r") }
            Edge { classes("inset-y-0", "right-0", "w-4", "bg-linear-to-l") }

            Button({
                classes(
                    "absolute", "bottom-3", "right-3", "z-20", "grid", "place-items-center", "rounded-lg", "py-1", "px-4", "border",
                    "border-slate-700", "bg-slate-950", "text-slate-300", "shadow-lg", "transition", "hover:bg-slate-800", "hover:text-slate-100",
                )
                onClick {
                    onViewportChange(null)
                }
            }) {
                Text("Reset View")
            }
        }
    }
}
