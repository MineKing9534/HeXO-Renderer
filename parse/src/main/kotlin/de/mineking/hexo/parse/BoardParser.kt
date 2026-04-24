package de.mineking.hexo.parse

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Player

interface BoardParser {
    suspend fun parse(notation: String): Board
}

object RectilinearNotationParser : BoardParser {
    override suspend fun parse(notation: String): Board {
        val board = Board()
        val cursor = Cursor(board)

        val buffer = StringBuilder()
        notation.forEachIndexed { offset, ch ->
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
}

fun BoardParser.cached(cacheSize: Long = 4): BoardParser = when (this) {
    is CachingBoardParser -> this
    else -> CachingBoardParser(this, cacheSize)
}

private class CachingBoardParser(val delegate: BoardParser, cacheSize: Long) : BoardParser {
    private val cache = Caffeine.newBuilder().maximumSize(cacheSize).asCache<String, Board>()

    override suspend fun parse(notation: String) = cache.get(notation) { delegate.parse(it) }
}
