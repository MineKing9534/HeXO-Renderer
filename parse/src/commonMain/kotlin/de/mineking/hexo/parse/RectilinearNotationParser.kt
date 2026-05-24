package de.mineking.hexo.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.minus
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.requireHexo
import de.mineking.hexo.board.times
import de.mineking.hexo.core.CellOwner

object RectilinearNotationParser : BoardParser {
    override suspend fun parse(notation: String) = notation.parseRectilinearNotation()
}

fun String.parseRectilinearNotation(): Board {
    val board = Board()
    val cursor = Cursor(board)

    var state = ParserState.Normal
    val buffer = StringBuilder()

    forEachIndexed { offset, ch ->
        state = state.handleChar(ch, offset, cursor, buffer)
    }

    requireHexo(buffer.isEmpty()) { "Unterminated symbol at end of input" }
    return board
}

private enum class ParserState {
    Normal {
        override fun handleChar(ch: Char, offset: Int, cursor: Cursor, buffer: StringBuilder): ParserState {
            return when {
                ch.isDigit() -> GapDigits.handleChar(ch, offset, cursor, buffer)
                ch == '(' -> LineHighlight
                ch == '[' -> Label
                else -> {
                    cursor.handleNormalChar(ch, offset)
                    this
                }
            }
        }

        private fun Cursor.handleNormalChar(ch: Char, offset: Int) {
            when (ch) {
                ' ' -> return
                '/', '\n' -> {
                    newRow()
                    return
                }

                'x', 'X' -> configureCurrent { owner = CellOwner.X }
                'o', 'O' -> configureCurrent { owner = CellOwner.O }
                '.', '!' -> {}
                '-' -> step()
                else -> throw HexoNotationException("Unexpected character `$ch` at offset $offset")
            }

            if (ch.isUpperCase() || ch == '!') {
                configureCurrent { highlighted = true }
            }

            step()
        }
    },
    GapDigits {
        override fun handleChar(ch: Char, offset: Int, cursor: Cursor, buffer: StringBuilder): ParserState {
            if (ch.isDigit()) {
                buffer.append(ch)
                return this
            } else {
                cursor.step(buffer.toString().toInt())
                buffer.clear()

                return Normal.handleChar(ch, offset, cursor, buffer)
            }
        }
    },
    Label {
        override fun handleChar(ch: Char, offset: Int, cursor: Cursor, buffer: StringBuilder): ParserState {
            if (ch == ']') {
                requireHexo(buffer.length <= 3) { "Labels can be at most 3 characters long!" }
                cursor.configurePrevious { label = buffer.toString() }
                buffer.clear()

                return Normal
            } else {
                buffer.append(ch)
                return this
            }
        }
    },
    LineHighlight {
        private val pattern = """^([bdpq<>])\s*(\d*)\s*([xo]?)$""".toRegex()

        override fun handleChar(ch: Char, offset: Int, cursor: Cursor, buffer: StringBuilder): ParserState {
            if (ch == ')') {
                cursor.highlightLine(buffer.toString())
                buffer.clear()

                return Normal
            } else {
                buffer.append(ch)
                return this
            }
        }

        private fun Cursor.highlightLine(notation: String) {
            val match = pattern.matchEntire(notation)
            requireHexo(match != null) { "Invalid line highlight notation, use `[b,d,p,q,<,>]<length>?[x,o]?`" }

            val direction = Direction.fromSymbol(match.groupValues[1])
            val length = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 6
            val color = when (match.groupValues[3]) {
                "x" -> CellOwner.X
                "o" -> CellOwner.O
                else -> null
            }

            highlightLine(previousPosition, direction, length, color)
        }
    },
    ;

    abstract fun handleChar(ch: Char, offset: Int, cursor: Cursor, buffer: StringBuilder): ParserState
}

private class Cursor(private val board: Board) {
    companion object {
        private val STEP_DIRECTION = CellCoordinate(1, 0)
        private val NEWLINE_DIRECTION = CellCoordinate(0, 1)
    }

    var position = CellCoordinate.Zero
        private set

    val previousPosition get() = position - STEP_DIRECTION

    fun configureCurrent(block: Cell.() -> Unit) {
        board[position].block()
    }

    fun configurePrevious(block: Cell.() -> Unit) {
        requireHexo(position.q >= 1) { "This operations requires a cell in the current row!" }
        board[position - STEP_DIRECTION].block()
    }

    fun highlightLine(origin: CellCoordinate, direction: Direction, length: Int, color: CellOwner?) {
        board.highlightLine(origin, direction, length, color)
    }

    fun step(n: Int = 1) {
        position += STEP_DIRECTION * n
    }

    fun newRow() {
        position += NEWLINE_DIRECTION
        position = position.copy(q = 0)
    }
}
