package de.mineking.hexo.core

data class CellCoordinate(val q: Int, val r: Int)

class Board {
    val cells: Map<CellCoordinate, Cell>
        field = mutableMapOf<CellCoordinate, Cell>()

    operator fun get(q: Int, r: Int) = get(CellCoordinate(q, r))
    operator fun get(coordinate: CellCoordinate) = cells.getOrPut(coordinate) { Cell(null, false) }

    operator fun set(q: Int, r: Int, cell: Cell) = set(CellCoordinate(q, r), cell)
    operator fun set(coordinate: CellCoordinate, cell: Cell) {
        cells[coordinate] = cell
    }

    companion object {
        fun fromRectilinearNotation(input: String): Board {
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
                    '!' -> cursor.highlight()
                    else -> throw IllegalArgumentException("Unexpected character '$ch' at offset $offset")
                }

                cursor.step()
            }

            return board
        }
    }
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
