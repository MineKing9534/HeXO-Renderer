package de.mineking.hexo.parse.test

import de.mineking.hexo.core.Cell
import de.mineking.hexo.core.CellCoordinate
import de.mineking.hexo.core.Player
import de.mineking.hexo.parse.RectilinearNotationParser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BoardParserTest {
    @Test
    fun `parse compact simple`() = runTest {
        val board = RectilinearNotationParser.parse("x-x/o.o//x")
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
    fun `parse compact with single digit`() = runTest {
        val board = RectilinearNotationParser.parse("x2x/o1o//x")
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
    fun `parse compact with long number`() = runTest {
        val board = RectilinearNotationParser.parse("x10x")
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(Player.X),
                CellCoordinate(11, 0) to Cell(Player.X),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse multiline`() = runTest {
        val board = RectilinearNotationParser.parse(
            """
                x . .  x
                 o . o
                
                   x
            """.trimIndent(),
        )
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
