package de.mineking.hexo.board

import de.mineking.hexo.core.CellOwner

class Board {
    companion object {
        private const val WIN_MIN_LENGTH = 6
        private val directions = listOf(
            CellCoordinate(1, 0),
            CellCoordinate(0, 1),
            CellCoordinate(1, -1),
        )
    }

    val highlightedLines: List<HighlightLine>
        field = mutableListOf()

    val cells: Map<CellCoordinate, Cell>
        field = mutableMapOf()

    fun highlightLine(origin: CellCoordinate, direction: Direction, length: Int, color: CellOwner? = null) {
        highlightedLines += HighlightLine(origin, direction, length, color)
    }

    fun addHighlightLines(lines: List<HighlightLine>) {
        highlightedLines += lines
    }

    fun addCells(cells: Map<CellCoordinate, Cell>) {
        this.cells += cells
    }

    operator fun get(q: Int, r: Int) = get(CellCoordinate(q, r))
    operator fun get(coordinate: CellCoordinate) = cells.getOrPut(coordinate) { Cell(null, false) }

    operator fun set(q: Int, r: Int, cell: Cell) = set(CellCoordinate(q, r), cell)
    operator fun set(coordinate: CellCoordinate, cell: Cell) {
        cells[coordinate] = cell
    }

    override fun equals(other: Any?) = other is Board && cells == other.cells && highlightedLines == other.highlightedLines
    override fun hashCode(): Int {
        var result = highlightedLines.hashCode()
        result = 31 * result + cells.hashCode()
        return result
    }

    fun findWinningRows(): List<List<Pair<CellCoordinate, Cell>>> {
        val rows = mutableListOf<List<Pair<CellCoordinate, Cell>>>()

        for ((coordinate, cell) in cells) {
            val owner = cell.owner ?: continue

            for (direction in directions) {
                val previousCoordinate = coordinate - direction
                val previousOwner = cells[previousCoordinate]?.owner
                if (previousOwner == owner) continue

                val row = mutableListOf<Pair<CellCoordinate, Cell>>()
                var current = coordinate

                while (true) {
                    val currentCell = cells[current] ?: break
                    if (currentCell.owner != owner) break

                    row += current to currentCell
                    current += direction
                }

                if (row.size >= WIN_MIN_LENGTH) {
                    rows += row
                }
            }
        }

        return rows
    }
}

fun Board.clone() = Board().apply {
    addHighlightLines(this@clone.highlightedLines)
    addCells(this@clone.cells.mapValues { (_, cell) -> cell.copy() })
}

fun Board.merge(other: Board, overrideOwner: Boolean = false): Board {
    val cells = cells.toMutableMap()
    other.cells.forEach { (coordinate, cell) ->
        cells.merge(coordinate, cell) { old, new ->
            requireHexo(overrideOwner || old.owner == null || new.owner == null) {
                "At $coordinate: Owner override is disabled but both cells have an owner defined"
            }
            Cell(
                new.owner ?: old.owner,
                highlighted = old.highlighted || new.highlighted,
                focussed = old.focussed || new.focussed,
                turn = new.turn ?: old.turn,
            )
        }
    }
    return Board().apply {
        addCells(cells)
        addHighlightLines(this@merge.highlightedLines)
        addHighlightLines(other.highlightedLines)
    }
}

@IgnorableReturnValue
private fun <K, V> MutableMap<K, V>.merge(key: K, value: V, merge: (V, V) -> V?): V? {
    val old = this[key]
    val new = if (old == null) value else merge(old, value)

    if (new == null) {
        this -= key
    } else {
        this[key] = new
    }

    return new
}
