package de.mineking.hexo.api

import de.mineking.hexo.api.formation.Formation
import de.mineking.hexo.api.game.FinishedGame
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.focusWinningRows

fun FinishedGame.asBoard(
    maxMoves: Int = moves.size,
    showTurnNumber: Boolean = false,
    focusWinningRows: Boolean = true,
): Board = MutableBoard().apply {
    val maxMoves = maxMoves.coerceIn(0, moves.size)
    repeat(maxMoves) {
        val move = moves[it]

        val cell = this[move.q, move.r]
        cell.owner = move.player.color

        val turn = (it + 1) / 2
        if (showTurnNumber) cell.turn = turn

        if (turn == maxMoves / 2) cell.focused = true
    }

    if (focusWinningRows) {
        focusWinningRows()
    }
}

fun Formation.asBoard(
    showTurnNumber: Boolean = false,
    focusWinningRows: Boolean = true,
): Board = MutableBoard().apply {
    gamePosition.cells.forEachIndexed { index, move ->
        val cell = this[move.q, move.r]
        cell.owner = move.player

        val turn = (index + 1) / 2
        if (showTurnNumber) cell.turn = turn

        if (turn == gamePosition.cells.size / 2) cell.focused = true
    }

    if (focusWinningRows) {
        focusWinningRows()
    }
}
