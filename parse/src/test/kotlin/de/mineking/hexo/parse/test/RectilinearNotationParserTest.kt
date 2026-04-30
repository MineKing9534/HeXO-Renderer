package de.mineking.hexo.parse.test

import de.mineking.hexo.core.Cell
import de.mineking.hexo.core.CellCoordinate
import de.mineking.hexo.core.Direction
import de.mineking.hexo.core.HighlightLine
import de.mineking.hexo.core.Player
import de.mineking.hexo.parse.parseRectilinearNotation
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class RectilinearNotationParserTest {
    @Test
    fun `parse compact simple`() {
        val board = "x-x/o.o//x".parseRectilinearNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(Player.X),
                CellCoordinate(3, 0) to Cell(Player.X),
                CellCoordinate(0, 1) to Cell(Player.O),
                CellCoordinate(2, 1) to Cell(Player.O),
                CellCoordinate(0, 3) to Cell(Player.X),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse compact with single digit`() {
        val board = "x2x/o1o//x".parseRectilinearNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(Player.X),
                CellCoordinate(3, 0) to Cell(Player.X),
                CellCoordinate(0, 1) to Cell(Player.O),
                CellCoordinate(2, 1) to Cell(Player.O),
                CellCoordinate(0, 3) to Cell(Player.X),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse compact with long number`() {
        val board = "x10x".parseRectilinearNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(Player.X),
                CellCoordinate(11, 0) to Cell(Player.X),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse multiline`() {
        val board = """
            x . .  x
             o . o
            
               x
        """.trimIndent().parseRectilinearNotation()

        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(Player.X),
                CellCoordinate(3, 0) to Cell(Player.X),
                CellCoordinate(0, 1) to Cell(Player.O),
                CellCoordinate(2, 1) to Cell(Player.O),
                CellCoordinate(0, 3) to Cell(Player.X),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse with cell highlight`() {
        val board = "x X - ! O".parseRectilinearNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(Player.X),
                CellCoordinate(1, 0) to Cell(Player.X, highlighted = true),
                CellCoordinate(4, 0) to Cell(null, highlighted = true),
                CellCoordinate(5, 0) to Cell(Player.O, highlighted = true),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse line highlight`() {
        val board = "..(<-3x)/x/o(<\\)".parseRectilinearNotation()

        assertEquals(
            mapOf(
                CellCoordinate(0, 1) to Cell(Player.X),
                CellCoordinate(0, 2) to Cell(Player.O),
            ),
            board.cells,
        )

        assertEquals(
            listOf(
                HighlightLine(CellCoordinate(1, 0), Direction.Left, length = 3, color = Player.X),
                HighlightLine(CellCoordinate(0, 2), Direction.TopLeft, length = 4, color = null),
            ),
            board.highlightedLines,
        )
    }

    @Test
    fun `parse with label`() {
        val board = """
            x . [a] . o [b]
        """.trimIndent().parseRectilinearNotation()

        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(Player.X),
                CellCoordinate(1, 0) to Cell(null, label = "a"),
                CellCoordinate(3, 0) to Cell(Player.O, label = "b"),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse with label on empty row`() {
        val e = assertThrows<IllegalArgumentException> {
            " x/[a]".parseRectilinearNotation()
        }

        assertEquals("This operations requires a cell in the current row!", e.message)
    }

    @Test
    fun `parse with unterminated label at eof`() {
        val e = assertThrows<IllegalArgumentException> {
            "x.[ab".parseRectilinearNotation()
        }
        assertEquals("Unterminated symbol at end of input", e.message)
    }
}
