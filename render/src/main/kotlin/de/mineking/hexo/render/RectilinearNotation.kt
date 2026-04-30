@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.render

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Cell
import de.mineking.hexo.core.CellCoordinate
import de.mineking.hexo.core.Player
import kotlin.math.max
import kotlin.math.min

enum class RectilinearNotationType(
    val columnSeparator: String,
    val rowSeparator: String,
) {
    Compact("", "/") {
        private val regex = "\\.{2,}".toPattern()

        override fun String.postprocess(): String {
            val result = StringBuilder()
            val matcher = regex.matcher(this)

            while (matcher.find()) {
                val value = matcher.group()
                matcher.appendReplacement(result, if (value.length == 2) "-" else value.length.toString())
            }
            matcher.appendTail(result)

            return result.toString()
        }
    },
    Multiline(" ", "\n"),
    ;

    open fun String.postprocess() = this
}

class RectilinearNotationBoardRenderer(val type: RectilinearNotationType) : BoardRenderer<String> {
    override suspend fun Board.render() = renderRectilinearNotation(type)
}

fun Board.renderRectilinearNotation(type: RectilinearNotationType) = buildString {
    val winningCells = findWinningRows().flatten().map { it.first }.toSet()
    val lines = cells.entries
        .filter { (_, cell) -> cell.owner != null || cell.highlighted || cell.label.isNotBlank() }
        .groupBy { (coordinate, _) -> coordinate.r }
        .mapValues { (_, cells) ->
            val line = cells.associate { (coordinate, cell) -> coordinate.q to cell }
            val size = cells.boardSize { (coordinate, _) -> coordinate }
            line to size
        }

    val minQ = lines.values.minOf { (_, size) -> size.minQ }
    val minR = lines.keys.min()
    val maxR = lines.keys.max()

    (minR..maxR).forEachIndexed { i, r ->
        val (line, size) = lines[r] ?: run {
            append(type.rowSeparator)
            return@forEachIndexed
        }

        append(type.columnSeparator.repeat(i))
        for (q in min(minQ, size.minQ)..size.maxQ) {
            appendCell(line[q]?.let { it.copy(focussed = it.focussed || CellCoordinate(q, r) in winningCells) })
            if (q < size.maxQ) append(type.columnSeparator)
        }

        if (r < maxR) append(type.rowSeparator)
    }
}.let { type.run { it.postprocess() } }

private fun StringBuilder.appendCell(cell: Cell?) {
    val highlight = cell != null && cell.highlighted

    append(when (cell?.owner) {
        Player.X -> if (highlight) "X" else "x"
        Player.O -> if (highlight) "O" else "o"
        null -> if (highlight) "!" else "."
    })

    if (!cell?.label.isNullOrBlank()) {
        append("[${cell.label}]")
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
