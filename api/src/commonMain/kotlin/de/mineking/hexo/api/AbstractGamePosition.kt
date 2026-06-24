package de.mineking.hexo.api

import de.mineking.hexo.api.game.Move
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.focusWinningRows

interface AbstractGamePosition {
    val moves: List<Move>
}

fun AbstractGamePosition.asBoard(
    maxMoves: Int = moves.size,
    showTurnNumbers: Boolean = false,
    focusWinningRows: Boolean = true,
): Board = MutableBoard().apply {
    val maxMoves = maxMoves.coerceIn(0, moves.size)
    repeat(maxMoves) {
        val move = moves[it]

        val cell = this[move.coordinate]
        cell.owner = move.owner

        val turn = (it + 1) / 2
        if (showTurnNumbers) cell.turn = turn

        if (turn == maxMoves / 2) cell.focused = true
    }

    if (focusWinningRows) {
        focusWinningRows()
    }
}
