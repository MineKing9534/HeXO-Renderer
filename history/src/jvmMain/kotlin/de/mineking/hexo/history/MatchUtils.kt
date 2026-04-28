package de.mineking.hexo.history

import de.mineking.hexo.board.Board

fun Match.asBoard(maxMoves: Int = moveCount, showTurnNumber: Boolean = false) = Board().apply {
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
