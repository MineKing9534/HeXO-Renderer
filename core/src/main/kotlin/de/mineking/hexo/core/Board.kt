package de.mineking.hexo.core

import java.util.Objects
import kotlin.collections.getOrPut
import kotlin.collections.toMutableMap

class Board(initial: MutableMap<CellCoordinate, Cell> = mutableMapOf()) {
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
        field = initial

    fun highlightLine(origin: CellCoordinate, direction: Direction, length: Int, color: Player? = null) {
        highlightedLines += HighlightLine(origin, direction, length, color)
    }

    operator fun get(q: Int, r: Int) = get(CellCoordinate(q, r))
    operator fun get(coordinate: CellCoordinate) = cells.getOrPut(coordinate) { Cell(null, false) }

    operator fun set(q: Int, r: Int, cell: Cell) = set(CellCoordinate(q, r), cell)
    operator fun set(coordinate: CellCoordinate, cell: Cell) {
        cells[coordinate] = cell
    }

    override fun hashCode() = Objects.hash(cells, highlightedLines)
    override fun equals(other: Any?) = other is Board && cells == other.cells && highlightedLines == other.highlightedLines

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

fun Board.merge(other: Board, overrideOwner: Boolean = false): Board {
    val cells = cells.toMutableMap()
    other.cells.forEach { (coordinate, cell) ->
        cells.merge(coordinate, cell) { old, new ->
            require(overrideOwner || old.owner == null || new.owner == null) {
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
    return Board(cells)
}
