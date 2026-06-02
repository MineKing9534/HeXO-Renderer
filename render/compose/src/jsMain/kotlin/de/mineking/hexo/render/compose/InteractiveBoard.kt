@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.render.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.clone
import de.mineking.hexo.board.contains
import de.mineking.hexo.board.distanceTo
import de.mineking.hexo.board.plus
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.render.image.BasicTheme
import de.mineking.hexo.render.image.CanvasFont
import de.mineking.hexo.render.image.DefaultCanvasFont
import de.mineking.hexo.render.image.Theme
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.w3c.dom.HTMLCanvasElement

sealed interface BoardInteraction {
    data class PlaceCell(val coordinate: CellCoordinate) : BoardInteraction

    sealed interface HighlightBoardInteraction : BoardInteraction {
        fun apply(board: MutableBoard)
    }

    data class HighlightCell(val coordinate: CellCoordinate, val highlight: CellHighlight?) : HighlightBoardInteraction {
        override fun apply(board: MutableBoard) {
            board[coordinate].highlight = highlight
        }
    }

    data class HighlightLine(val line: LineHighlight, val isRemove: Boolean) : HighlightBoardInteraction {
        override fun apply(board: MutableBoard) {
            if (!isRemove) {
                board.lineHighlights += line
            } else {
                board.lineHighlights -= line
            }
        }
    }
}

private fun Board.findTopHighlightLineAt(coordinate: CellCoordinate) = lineHighlights
    .lastOrNull { coordinate in it }

@Composable
fun InteractiveBoard(
    board: Board,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport) -> Unit,
    onBoardInteraction: (BoardInteraction) -> Unit,
    theme: Theme = BasicTheme.Default,
    font: CanvasFont = DefaultCanvasFont,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    content: ContentBuilder<HTMLCanvasElement>? = null,
) {
    var temporaryLine by remember { mutableStateOf<LineHighlight?>(null) }
    val effectiveBoard = remember(board, temporaryLine) {
        val temporaryLine = temporaryLine
        if (temporaryLine == null) {
            board
        } else {
            board.clone().apply {
                lineHighlights += temporaryLine
            }
        }
    }

    val onBoardInteraction by rememberUpdatedState(onBoardInteraction)

    RawBoard(
        board = effectiveBoard,
        viewport = viewport,
        onViewportChange = onViewportChange,
        theme = theme,
        font = font,
        onCellClick = {
            onBoardInteraction(BoardInteraction.PlaceCell(it))
        },
        onBoardRightClick = { (from, to, phase, modifiers) ->
            if (phase == BoardRightClickPhase.Abort) {
                temporaryLine = null
                return@RawBoard
            }

            val color = when {
                modifiers.ctrlKey -> CellOwner.X
                modifiers.altKey || modifiers.shiftKey -> CellOwner.O
                else -> null
            }

            if (from == to && phase == BoardRightClickPhase.Commit) {
                val line = board.findTopHighlightLineAt(to)
                if (line == null) {
                    val highlight = if (board.cells[to]?.highlight == null) CellHighlight(color) else null
                    onBoardInteraction(BoardInteraction.HighlightCell(to, highlight))
                } else {
                    onBoardInteraction(BoardInteraction.HighlightLine(line, isRemove = true))
                }
                temporaryLine = null
            } else {
                val (direction, length) = from.findClosestLineTo(to)
                val line = LineHighlight(from, direction, length, color)

                if (phase == BoardRightClickPhase.Commit) {
                    onBoardInteraction(BoardInteraction.HighlightLine(line, isRemove = false))
                    temporaryLine = null
                } else if (from != to) {
                    temporaryLine = line
                } else {
                    temporaryLine = null
                }
            }
        },
        attrs = attrs,
        content = content,
    )
}

@Composable
fun BoardView(
    board: Board,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport) -> Unit,
    theme: Theme = BasicTheme.Default,
    font: CanvasFont = DefaultCanvasFont,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    content: ContentBuilder<HTMLCanvasElement>? = null,
) {
    var overlay by remember { mutableStateOf(Board()) }
    val effectiveBoard = remember(board, overlay) { board + overlay }

    InteractiveBoard(
        board = effectiveBoard,
        viewport = viewport,
        onViewportChange = onViewportChange,
        onBoardInteraction = { interaction ->
            if (interaction is BoardInteraction.HighlightBoardInteraction) {
                overlay = overlay.clone().also {
                    interaction.apply(it)
                }
            }
        },
        theme = theme,
        font = font,
        attrs = attrs,
        content = content,
    )
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

    return bestDirection to (bestLength + 1).coerceAtMost(LineHighlight.MAX_LENGTH)
}
