package de.mineking.hexo.board

import de.mineking.hexo.core.CellOwner

interface Board {
    companion object {
        const val WIN_MIN_LENGTH = 6
    }

    val lineHighlights: List<LineHighlight>
    val cells: Map<CellCoordinate, Cell>
}

fun Board(): Board = MutableBoard()

class MutableBoard : Board {
    override val lineHighlights = mutableListOf<LineHighlight>()
    override val cells = mutableMapOf<CellCoordinate, MutableCell>()

    fun highlightLine(origin: CellCoordinate, direction: Direction, length: Int, color: CellOwner? = null) {
        lineHighlights += LineHighlight(origin, direction, length, color)
    }

    operator fun get(q: Int, r: Int) = get(CellCoordinate(q, r))
    operator fun get(coordinate: CellCoordinate) = cells.getOrPut(coordinate) { MutableCell() }

    operator fun set(q: Int, r: Int, cell: MutableCell) = set(CellCoordinate(q, r), cell)
    operator fun set(coordinate: CellCoordinate, cell: MutableCell) {
        cells[coordinate] = cell
    }

    override fun equals(other: Any?) = other is Board && cells == other.cells && lineHighlights == other.lineHighlights
    override fun hashCode(): Int {
        var result = lineHighlights.hashCode()
        result = 31 * result + cells.hashCode()
        return result
    }
}

private val directions = listOf(
    CellCoordinate(1, 0),
    CellCoordinate(0, 1),
    CellCoordinate(1, -1),
)
fun Board.findWinningRows(): List<List<Pair<CellCoordinate, Cell>>> {
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

            if (row.size >= Board.WIN_MIN_LENGTH) {
                rows += row
            }
        }
    }

    return rows
}

@IgnorableReturnValue
fun MutableBoard.focusWinningRows() = apply {
    findWinningRows().forEach {
        it.forEach { (coordinate) ->
            this[coordinate].focused = true
        }
    }
}

fun Board.clone() = MutableBoard().apply {
    this.lineHighlights += this@clone.lineHighlights
    this.cells += this@clone.cells.mapValues { (_, cell) -> cell.copy() }
}

operator fun Board.plus(other: Board) = merge(other)
fun Board.merge(other: Board, overrideOwner: Boolean = false): Board {
    val cells = mutableMapOf<CellCoordinate, MutableCell>()
    this.cells.forEach { (coordinate, cell) -> cells[coordinate] = cell.copy() }

    other.cells.forEach { (coordinate, cell) ->
        cells.merge(coordinate, cell.copy()) { old, new ->
            requireHexo(overrideOwner || old.owner == null || new.owner == null) {
                "At $coordinate: Owner override is disabled but both cells have an owner defined"
            }
            MutableCell(
                new.owner ?: old.owner,
                highlight = old.highlight ?: new.highlight,
                focused = old.focused || new.focused,
                turn = new.turn ?: old.turn,
            )
        }
    }
    return MutableBoard().apply {
        this.cells += cells
        this.lineHighlights += this@merge.lineHighlights
        this.lineHighlights += other.lineHighlights
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
