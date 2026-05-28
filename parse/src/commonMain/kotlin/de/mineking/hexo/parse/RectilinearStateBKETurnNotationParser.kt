package de.mineking.hexo.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.merge

object RectilinearStateBKETurnNotationParser : BoardParser {
    override suspend fun parse(notation: String) = notation.parseRectilinearStateBKETurnNotation()
}

fun String.parseRectilinearStateBKETurnNotation(): Board {
    val parts = split(",\\s*".toRegex(), limit = 2)

    return if (parts.size == 1) {
        parseBKENotationOrNull(implicitOrigin = true) ?: parseRectilinearNotation()
    } else {
        val (rectilinear, bke) = parts

        val originalState = rectilinear.parseRectilinearNotation()
        val additionalMoves = bke.parseBKENotationOrNull(implicitOrigin = false)
            ?: throw HexoNotationException("Invalid BKE notation format, use `[b,d,p,q,<,>][CW,CCW]? ...`")

        originalState.merge(additionalMoves)
    }
}
