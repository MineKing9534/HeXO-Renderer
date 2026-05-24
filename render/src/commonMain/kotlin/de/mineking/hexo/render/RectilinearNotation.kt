package de.mineking.hexo.render

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.HighlightLine
import de.mineking.hexo.core.CellOwner
import kotlin.math.min

enum class RectilinearNotationType(
    val columnSeparator: String,
    val rowSeparator: String,
) {
    Compact("", "/") {
        private val regex = "\\.{2,}".toRegex()

        override fun String.postprocess() = regex.replace(this) {
            if (it.value.length == 2) "-" else it.value.length.toString()
        }
    },
    Multiline(" ", "\n"),
    ;

    open fun String.postprocess() = this
}

class RectilinearNotationBoardRenderer(val type: RectilinearNotationType) : BoardRenderer<String> {
    override suspend fun Board.render() = renderRectilinearNotation(type)
}

fun Board.renderRectilinearNotation(type: RectilinearNotationType): String {
    val highlightLines = highlightedLines.groupBy { it.start }
    val renderCells = (highlightLines.keys.associateWith { Cell() } + cells)
        .filter { (coordinate, cell) -> coordinate in highlightLines || cell.shouldRender() }

    if (renderCells.isEmpty()) return ""

    val rows = renderCells.entries
        .groupBy { (coordinate, _) -> coordinate.r }
        .mapValues { (_, cells) -> cells.associate { (coordinate, cell) -> coordinate.q to cell } }

    val minQ = renderCells.keys.minOf { it.q }
    val minR = rows.keys.min()
    val maxR = rows.keys.max()

    return buildString {
        for (r in minR..maxR) {
            rows[r]?.let { row ->
                append(type.columnSeparator.repeat(r - minR))
                appendRow(r, row, minQ, highlightLines, type)
            }

            if (r < maxR) {
                append(type.rowSeparator)
            }
        }
    }.let { type.run { it.postprocess() } }
}

private fun Cell.shouldRender() = owner != null || highlighted || label.isNotBlank()

private fun StringBuilder.appendRow(
    r: Int,
    row: Map<Int, Cell>,
    minQ: Int,
    highlightLines: Map<CellCoordinate, List<HighlightLine>>,
    type: RectilinearNotationType,
) {
    val maxQ = row.keys.max()

    for (q in min(minQ, row.keys.min())..maxQ) {
        val coordinate = CellCoordinate(q, r)
        appendCell(row[q])
        highlightLines[coordinate]?.forEach { append(it.render()) }

        if (q < maxQ) {
            append(type.columnSeparator)
        }
    }
}

private fun HighlightLine.render(): String {
    val owner = when (color) {
        CellOwner.X -> "x"
        CellOwner.O -> "o"
        null -> ""
    }

    return "(${direction.symbol}$length$owner)"
}

private fun StringBuilder.appendCell(cell: Cell?) {
    val highlighted = cell?.highlighted == true

    append(when (cell?.owner) {
        CellOwner.X -> if (highlighted) "X" else "x"
        CellOwner.O -> if (highlighted) "O" else "o"
        null -> if (highlighted) "!" else "."
    })

    if (!cell?.label.isNullOrBlank()) {
        append("[${cell.label}]")
    }
}
