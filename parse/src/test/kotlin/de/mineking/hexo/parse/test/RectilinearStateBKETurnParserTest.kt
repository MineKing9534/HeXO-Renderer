package de.mineking.hexo.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.HighlightLine
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.parse.parseRectilinearStateBKETurnNotation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RectilinearStateBKETurnParserTest {
    @Test
    fun `test 1`() {
        val board = ".x/xx, b@(1,0): o A0 A1 x B3.1 B3.2".parseRectilinearStateBKETurnNotation()
        assertEquals(
            mapOf(
                CellCoordinate(1, 0) to Cell(CellOwner.X),
                CellCoordinate(0, 1) to Cell(CellOwner.X),
                CellCoordinate(1, 1) to Cell(CellOwner.X),

                CellCoordinate(1, -1) to Cell(CellOwner.O, turn = 1),
                CellCoordinate(2, -1) to Cell(CellOwner.O, turn = 1),

                CellCoordinate(-1, 2) to Cell(CellOwner.X, turn = 2, focussed = true),
                CellCoordinate(0, 2) to Cell(CellOwner.X, turn = 2, focussed = true),
            ),
            board.cells,
        )
    }

    @Test
    fun `test highlight lines`() {
        val board = "X(>)x, > x A1 B1".parseRectilinearStateBKETurnNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X, highlighted = true),
                CellCoordinate(1, 0) to Cell(CellOwner.X),
                CellCoordinate(0, 1) to Cell(CellOwner.X, focussed = true, turn = 1),
                CellCoordinate(1, 1) to Cell(CellOwner.X, focussed = true, turn = 1),
            ),
            board.cells,
        )
        assertEquals(
            listOf(
                HighlightLine(CellCoordinate.Zero, Direction.Right, 6, null),
            ),
            board.highlightedLines,
        )
    }
}
