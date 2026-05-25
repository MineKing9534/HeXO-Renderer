package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.createRepositories
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.HighlightLine
import de.mineking.hexo.board.clone
import de.mineking.hexo.board.contains
import de.mineking.hexo.board.distanceTo
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.parse.parseRectilinearStateBKETurnNotation
import de.mineking.hexo.render.compose.Board
import de.mineking.hexo.render.compose.BoardRightClickEvent
import de.mineking.hexo.render.compose.BoardViewport
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.MouseEvent
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
    var temporaryLine by remember { mutableStateOf<HighlightLine?>(null) }

    val transformedBoard = remember(board, temporaryLine) {
        board.clone().apply {
            val temporaryLine = temporaryLine
            if (temporaryLine != null) highlightedLines += temporaryLine
        }
    }

    Div({
        classes("flex", "h-screen", "w-screen", "overflow-hidden", "bg-slate-950", "font-sans", "text-slate-100")
    }) {
        BoardPane(
            board = transformedBoard,
            viewport = viewport,
            onViewportChange = { viewport = it },
            onCellClick = {
                val updated = board.clone()
                updated[it].owner = CellOwner.X
                board = updated
            },
            onBoardRightClick = { event ->
                handleBoardRightClick(board, event, onTemporaryLineUpdate = { temporaryLine = it }, onBoardUpdate = { board = it })
            },
        )
        Sidebar(
            formationRepository = repositories.formations,
            finishedGameRepository = repositories.finishedGames,
            board = transformedBoard,
            onBoardChange = { cause, updated ->
                board = updated
                if (cause == BoardUpdateCause.Import) {
                    viewport = null
                }
            },
        )
    }
}

private fun MouseEvent.handleBoardRightClick(
    board: HexoBoard,
    event: BoardRightClickEvent,
    onTemporaryLineUpdate: (HighlightLine?) -> Unit,
    onBoardUpdate: (HexoBoard) -> Unit,
) {
    val (from, to, final) = event

    val updated = board.clone()
    val color = when {
        ctrlKey -> CellOwner.X
        altKey -> CellOwner.O
        else -> null
    }

    if (from == to && final) {
        if (!updated.removeHighlightLinesAt(to)) {
            if (updated[to].highlight != null) {
                updated[to].highlight = null
            } else {
                updated[to].highlight = CellHighlight(color)
            }
        }
        onTemporaryLineUpdate(null)
    } else {
        val (direction, length) = from.findClosestLineTo(to)
        val line = HighlightLine(from, direction, length, color)

        if (final) {
            updated.highlightedLines += line
            onTemporaryLineUpdate(null)
        } else if (from != to) {
            onTemporaryLineUpdate(line)
        } else {
            onTemporaryLineUpdate(null)
        }
    }
    onBoardUpdate(updated)
}

@Composable
private fun BoardPane(
    board: HexoBoard,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport?) -> Unit,
    onCellClick: (MouseEvent.(CellCoordinate) -> Unit)? = null,
    onBoardRightClick: (MouseEvent.(BoardRightClickEvent) -> Unit)? = null,
) {
    Div({ classes("min-w-0", "flex-1", "p-6") }) {
        Div({ classes("relative", "h-full", "overflow-hidden", "rounded-2xl", "border", "border-slate-800", "bg-slate-900", "shadow-2xl") }) {
            Board(
                board = board,
                viewport = viewport,
                onViewportChange = onViewportChange,
                onCellClick = onCellClick,
                onBoardRightClick = onBoardRightClick,
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

private fun CellCoordinate.findClosestLineTo(to: CellCoordinate): Pair<Direction, Int> {
    val maxLength = distanceTo(to)

    var bestDirection = Direction.Right
    var bestLength = 0
    var bestDistance = distanceTo(to)

    for (direction in Direction.entries) {
        for (length in 0..maxLength) {
            val end = CellCoordinate(
                q + direction.direction.q * length,
                r + direction.direction.r * length,
            )

            val dist = end.distanceTo(to)

            if (dist < bestDistance || (dist == bestDistance && length < bestLength)) {
                bestDirection = direction
                bestLength = length
                bestDistance = dist
            }
        }
    }

    return bestDirection to (bestLength + 1).coerceAtMost(HighlightLine.MAX_LENGTH)
}

private fun HexoBoard.removeHighlightLinesAt(coordinate: CellCoordinate) = highlightedLines
    .indexOfLast { line -> coordinate in line }
    .let {
        if (it == -1) {
            return@let false
        }

        highlightedLines.removeAt(it)
        true
    }
