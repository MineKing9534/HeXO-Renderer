package de.mineking.hexo.hds

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.hds.game.Move

interface AbstractGamePosition {
    val moves: List<Move>
}

fun AbstractGamePosition.asBoard(
    maxMoves: Int = moves.size,
    focusWinningRows: Boolean = true,
): Board = MutableBoard().apply {
    val maxMoves = maxMoves.coerceIn(0, moves.size)
    repeat(maxMoves) {
        val move = moves[it]

        val cell = this[move.coordinate]
        cell.owner = move.owner

        cell.turn = (it + 1) / 2
    }

    if (focusWinningRows) {
        focusWinningRows()
    }
}
