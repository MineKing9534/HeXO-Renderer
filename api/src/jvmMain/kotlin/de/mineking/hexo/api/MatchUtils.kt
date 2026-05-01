package de.mineking.hexo.api

import de.mineking.hexo.api.game.FinishedGame
import de.mineking.hexo.board.Board

fun FinishedGame.asBoard(maxMoves: Int = moveCount, showTurnNumber: Boolean = false) = Board().apply {
    val maxMoves = maxMoves.coerceIn(0, moveCount)
    repeat(maxMoves) {
        val move = moves[it]

        val cell = this[move.q, move.r]
        cell.owner = playerIdMappings[move.id]

        val turn = (it + 1) / 2
        if (showTurnNumber) cell.turn = turn

        if (turn == maxMoves / 2) cell.focussed = true
    }
}
