package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.createRepositories
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.clone
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.parse.parseCombinedHexoNotation
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.board.render.compose.InteractiveBoard
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.web.components.Dialog
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.readOnly
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.url.URLSearchParams

fun main() {
    val params = URLSearchParams(window.location.search)
    val initial = params.get("position")?.replace("_", "/") ?: ""
    window.history.pushState(null, "", window.location.pathname)

    val (initialBoard, initialError) = try {
        val board = when {
            initial.isBlank() -> Board()
            else -> initial.parseCombinedHexoNotation(focusWinningRows = false)
        }

        board to null
    } catch (e: HexoNotationException) {
        Board() to e.message
    }

    val rootElement = document.getElementById("root")!!
    rootElement.innerHTML = ""

    renderComposable(root = rootElement) {
        var error by remember { mutableStateOf(initialError) }

        val client = remember { BuildConfig.API_PROXY?.let { HexoApiClient(host = it, socketIOOptions = null) } }
        MainLayout(client, initialBoard)

        if (error != null) {
            Dialog(title = "Invalid Position", onClose = { error = null }) {
                TextArea {
                    value(error ?: "")
                    classes(
                        "w-full", "resize-y", "rounded-lg", "border-3", "p-3", "text-sm", "text-rose-100",
                        "outline-none", "transition", "font-mono", "bg-slate-950", "border-rose-400",
                    )
                    readOnly()
                }
            }
        }
    }
}

enum class CellPlacementMode {
    State,
    Turn,
}

@Composable
private fun MainLayout(client: HexoApiClient?, initialBoard: Board) {
    val repositories = remember(client) { client?.createRepositories() }

    val viewport = remember { mutableStateOf<BoardViewport?>(null) }
    val placementMode = remember {
        mutableStateOf(
            when {
                initialBoard.cells.values.any { it.owner != null } -> CellPlacementMode.Turn
                else -> CellPlacementMode.State
            },
        )
    }

    var board by remember { mutableStateOf(initialBoard) }
    val transformedBoard = remember(board) {
        board.clone().apply {
            val maxTurn = getMaxTurn()
            cells.values.forEach { it.focused = maxTurn != null && (it.turn == maxTurn) }

            focusWinningRows()
        }
    }

    Div({
        classes("flex", "h-dvh", "w-screen", "flex-col", "overflow-hidden", "bg-slate-950", "font-sans", "text-slate-100", "md:flex-row")
    }) {
        BoardPane(
            board = transformedBoard,
            viewport = viewport,
            onBoardInteraction = { interaction ->
                board = board.clone().also {
                    when (interaction) {
                        is BoardInteraction.PlaceCell -> it.placeCell(interaction.coordinate, placementMode.value)
                        is BoardInteraction.HighlightBoardInteraction -> interaction.apply(it)
                    }
                }
            },
        )
        Sidebar(
            repositories = repositories,
            placementMode = placementMode,
            board = transformedBoard,
            onBoardChange = { cause, updated ->
                board = updated
                if (cause == BoardUpdateCause.Import) {
                    viewport.value = null
                }
            },
        )
    }
}

private fun Board.getMaxTurn() = cells.values.maxOfOrNull { it.turn ?: -1 }?.takeIf { it >= 0 }

private fun MutableBoard.placeCell(coordinate: CellCoordinate, mode: CellPlacementMode) {
    val maxTurn = getMaxTurn()

    val currentCell = cells[coordinate]
    if (currentCell?.turn != null && currentCell.turn == maxTurn) {
        this[coordinate].owner = null
        this[coordinate].turn = null
        return
    }

    when (mode) {
        CellPlacementMode.State if currentCell?.turn == null -> {
            this[coordinate].owner = when (currentCell?.owner) {
                null -> CellOwner.X
                CellOwner.X -> CellOwner.O
                CellOwner.O -> null
            }
        }

        CellPlacementMode.Turn -> {
            if (currentCell?.owner != null) return

            val (player, turn) = findNextTurn()
            this[coordinate].apply {
                this.owner = player
                this.turn = turn
            }
        }

        else -> {}
    }
}

private fun Board.findNextTurn(): Pair<CellOwner, Int> {
    var hadPosition = false
    var turn = 0
    var isComplete = false
    var player = CellOwner.X

    cells.values.forEach { cell ->
        val cellOwner = cell.owner ?: return@forEach
        val cellTurn = cell.turn?.takeIf { it >= turn } ?: run {
            hadPosition = true
            return@forEach
        }

        if (cellTurn == turn) {
            isComplete = true
        } else {
            turn = cellTurn
            isComplete = false
            player = cellOwner
        }
    }

    if (turn == 0 && hadPosition) {
        turn = 1
        player = CellOwner.X
    } else if (isComplete) {
        turn++
        player = player.other
    }

    return player to turn
}

@Composable
private fun BoardPane(
    board: Board,
    viewport: MutableState<BoardViewport?>,
    onBoardInteraction: (BoardInteraction) -> Unit,
) {
    var viewport by viewport
    Div({ classes("min-h-0", "min-w-0", "flex-1", "p-3", "md:p-6") }) {
        Div({ classes("relative", "h-full", "overflow-hidden", "rounded-2xl", "border", "border-slate-800", "bg-slate-900", "shadow-2xl") }) {
            InteractiveBoard(
                board = board,
                viewport = viewport,
                onViewportChange = { viewport = it },
                onBoardInteraction = onBoardInteraction,
                attrs = {
                    attr("width", "1200")
                    attr("height", "900")
                    classes("block", "h-full", "w-full", "touch-none")
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
                    viewport = null
                }
            }) {
                Text("Reset View")
            }
        }
    }
}
