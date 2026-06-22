package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.plus

class CombinedHexoNotationParser(val focusWinningRows: Boolean = true) : BoardParser {
    override suspend fun parse(notation: String) = notation.parseCombinedHexoNotation(focusWinningRows)
}

fun String.parseCombinedHexoNotation(focusWinningRows: Boolean = true): Board {
    parseHTTTXNotationOrNull(focusWinningRows)?.let { return it }

    val parts = split(",\\s*".toRegex(), limit = 2)

    val board = if (parts.size == 1) {
        parseBKENotationOrNull(implicitOrigin = true, focusWinningRows = false) ?: parseRectilinearNotation(focusWinningRows = false)
    } else {
        val (rectilinear, bke) = parts

        val originalState = rectilinear.parseRectilinearNotation(focusWinningRows = false)
        val additionalMoves = bke.parseBKENotationOrNull(implicitOrigin = false, focusWinningRows = false)
            ?: throw HexoNotationException("Invalid BKE notation format, use `[b,d,p,q,<,>][CW,CCW]? ...`")

        originalState + additionalMoves
    }

    if (focusWinningRows) {
        (board as MutableBoard).focusWinningRows()
    }

    return board
}
