package de.mineking.hexo.parse

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.CellCoordinate
import de.mineking.hexo.core.merge

private val BKE_FORMAT = """^([-/\\<>]{2})(CW|CCW)?\s*(?:@\s*\((-?\d+),\s*(-?\d+)\))?\s*:\s*(.*)$""".toRegex()

object RectilinearStateBKETurnNotationParser : BoardParser {
    override suspend fun parse(notation: String) = notation.parseRectilinearStateBKETurnNotation()
}

fun String.parseRectilinearStateBKETurnNotation(): Board {
    val parts = split(",\\s*".toRegex(), limit = 2)

    return if (parts.size == 1) {
        parseDirectionalBKENotation(pure = true) ?: parseRectilinearNotation()
    } else {
        val (rectilinear, bke) = parts

        val originalState = rectilinear.parseRectilinearNotation()
        val additionalMoves = bke.parseDirectionalBKENotation(pure = false)
            ?: throw IllegalArgumentException("Invalid BKE notation format, use [->|\\>|</|<-|<\\|/>][CW|CCW]?@(q,r): ...")

        originalState.merge(additionalMoves)
    }
}

private fun String.parseDirectionalBKENotation(pure: Boolean): Board? {
    val match = BKE_FORMAT.matchEntire(this) ?: return null

    val (_, zeroOffsetLineSymbol, chiralitySymbol, originQ, originR, content) = match.groupValues
    val zeroOffsetLine = ZeroOffsetLine.fromSymbol(zeroOffsetLineSymbol)
    val chirality = if (chiralitySymbol.isNotEmpty()) Chirality.fromSymbol(chiralitySymbol) else Chirality.Clockwise
    val origin = if (pure) {
        require(originR.isEmpty()) { "Origin cannot be specified in pure BKE notation" }
        null
    } else {
        if (originR.isNotEmpty()) CellCoordinate(originQ.toInt(), originR.toInt()) else CellCoordinate.Zero
    }

    return content.parseBKENotation(origin, zeroOffsetLine, chirality)
}

private operator fun <T> List<T>.component6() = get(5)
