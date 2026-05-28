package de.mineking.hexo.api

import de.mineking.hexo.api.formation.Formation
import de.mineking.hexo.api.game.FinishedGame
import de.mineking.hexo.board.Board

fun FinishedGame.asBoard(maxMoves: Int = moves.size, showTurnNumber: Boolean = false) = Board().apply {
    val maxMoves = maxMoves.coerceIn(0, moves.size)
    repeat(maxMoves) {
        val move = moves[it]

        val cell = this[move.q, move.r]
        cell.owner = move.player.color

        val turn = (it + 1) / 2
        if (showTurnNumber) cell.turn = turn

        if (turn == maxMoves / 2) cell.focused = true
    }
}

fun Formation.asBoard(showTurnNumber: Boolean = false) = Board().apply {
    gamePosition.cells.forEachIndexed { index, move ->
        val cell = this[move.q, move.r]
        cell.owner = move.player

        val turn = (index + 1) / 2
        if (showTurnNumber) cell.turn = turn

        if (turn == gamePosition.cells.size / 2) cell.focused = true
    }
}
