package de.mineking.hexo.core

data class CellCoordinate(val q: Int, val r: Int)

operator fun CellCoordinate.plus(other: CellCoordinate) = CellCoordinate(q + other.q, r + other.r)
operator fun CellCoordinate.minus(other: CellCoordinate) = CellCoordinate(q - other.q, r - other.r)

class Board {
    companion object {
        private const val WIN_MIN_LENGTH = 6
        private val directions = listOf(
            CellCoordinate(1, 0),
            CellCoordinate(0, 1),
            CellCoordinate(1, -1),
        )
    }

    val cells: Map<CellCoordinate, Cell>
        field = mutableMapOf<CellCoordinate, Cell>()

    operator fun get(q: Int, r: Int) = get(CellCoordinate(q, r))
    operator fun get(coordinate: CellCoordinate) = cells.getOrPut(coordinate) { Cell(null, false) }

    operator fun set(q: Int, r: Int, cell: Cell) = set(CellCoordinate(q, r), cell)
    operator fun set(coordinate: CellCoordinate, cell: Cell) {
        cells[coordinate] = cell
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

fun Board.Companion.fromRectilinearNotation(input: String): Board {
    val board = Board()
    val cursor = Cursor(board)

    input.forEachIndexed { offset, ch ->
        when (ch) {
            ' ' -> return@forEachIndexed
            '/', '\n' -> { cursor.newRow(); return@forEachIndexed }
            'x' -> cursor.set(Player.X)
            'X' -> { cursor.set(Player.X); cursor.highlight() }
            'o' -> cursor.set(Player.O)
            'O' -> { cursor.set(Player.O); cursor.highlight() }
            '.' -> {}
            '-' -> cursor.step()
            '!' -> cursor.highlight()
            else -> throw IllegalArgumentException("Unexpected character '$ch' at offset $offset")
        }

        cursor.step()
    }

    return board
}

private class Cursor(private val board: Board) {
    private var q = 0
    private var r = 0

    fun set(owner: Player) {
        board[q, r].owner = owner
    }

    fun highlight() {
        board[q, r].highlighted = true
    }

    fun step() {
        q++
    }

    fun newRow() {
        r++
        q = 0
    }
}
