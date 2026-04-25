package de.mineking.hexo.parse

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Player

object RectilinearNotationParser : BoardParser {
    override suspend fun parse(notation: String) = notation.parseRectilinearNotation()
}

fun String.parseRectilinearNotation(): Board {
    val board = Board()
    val cursor = Cursor(board)

    val buffer = StringBuilder()
    forEachIndexed { offset, ch ->
        if (ch.isDigit()) {
            buffer.append(ch)
        } else {
            if (buffer.isNotEmpty()) {
                cursor.step(buffer.toString().toInt())
                buffer.clear()
            }

            when (ch) {
                ' ' -> return@forEachIndexed
                '/', '\n' -> {
                    cursor.newRow()
                    return@forEachIndexed
                }

                'x', 'X' -> cursor.set(Player.X)
                'o', 'O' -> cursor.set(Player.O)
                '.', '!' -> {}
                '-' -> cursor.step()
                else -> throw IllegalArgumentException("Unexpected character '$ch' at offset $offset")
            }

            if (ch.isUpperCase() || ch == '!') {
                cursor.highlight()
            }

            cursor.step()
        }
    }

    require(board.cells.values.any { it.owner != null })
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

    fun step(n: Int = 1) {
        q += n
    }

    fun newRow() {
        r++
        q = 0
    }
}
