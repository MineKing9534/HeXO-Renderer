package de.mineking.hexo.render

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.CellCoordinate
import de.mineking.hexo.core.Player
import kotlin.math.max
import kotlin.math.min

enum class RectilinearNotationType(
   val columnSeparator: String,
   val rowSeparator: String,
   val emptyCellChar: String,
) {
    Compact("", "/", "."),
    Multiline(" ", "\n", " "),
}

fun Board.renderAsText(type: RectilinearNotationType) = buildString {
    val lines = cells.entries
        .filter { (_, cell) -> cell.owner != null }
        .groupBy { (coordinate, _ ) -> coordinate.r }
        .mapValues { (_, cells) -> cells.associate { (coordinate, cell) -> coordinate.q to cell } to cells.boardSize { (coordinate, _) -> coordinate } }

    val minQ = lines.values.minOf { (_, size) -> size.minQ }
    val minR = lines.keys.min()
    val maxR = lines.keys.max()

    (minR..maxR).forEachIndexed { i, r ->
        val (line, size) = lines[r] ?: run { append(type.rowSeparator); return@forEachIndexed }

        append(type.columnSeparator.repeat(i))
        for (q in min(minQ, size.minQ)..size.maxQ) {
            append(when (line[q]?.owner) {
                Player.X -> "x"
                Player.O -> "o"
                null -> type.emptyCellChar
            })
            if (q < size.maxQ) append(type.columnSeparator)
        }

        if (r < maxR) append(type.rowSeparator)
    }
}

private data class BoardSize(val minQ: Int, val maxQ: Int)

private fun <T> Collection<T>.boardSize(extractor: (T) -> CellCoordinate): BoardSize {
    var minQ = Int.MAX_VALUE
    var maxQ = Int.MIN_VALUE

    forEach {
        val (q) = extractor(it)
        minQ = min(minQ, q)
        maxQ = max(maxQ, q)
    }

    return BoardSize(minQ, maxQ)
}
