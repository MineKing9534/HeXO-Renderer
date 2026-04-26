package de.mineking.hexo.parse.test

import de.mineking.hexo.core.Cell
import de.mineking.hexo.core.CellCoordinate
import de.mineking.hexo.core.Player
import de.mineking.hexo.parse.parseRectilinearNotation
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
}
