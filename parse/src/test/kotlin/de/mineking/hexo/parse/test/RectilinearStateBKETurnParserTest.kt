package de.mineking.hexo.parse.test

import de.mineking.hexo.core.Cell
import de.mineking.hexo.core.CellCoordinate
import de.mineking.hexo.core.Player
import de.mineking.hexo.parse.parseRectilinearStateBKETurnNotation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RectilinearStateBKETurnParserTest {
    @Test
    fun `test 1`() {
        val board = ".x/xx, <\\@(1,0): o A0 A1 x B3.1 B3.2".parseRectilinearStateBKETurnNotation()
        assertEquals(
            mapOf(
                CellCoordinate(1, 0) to Cell(Player.X),
                CellCoordinate(0, 1) to Cell(Player.X),
                CellCoordinate(1, 1) to Cell(Player.X),

                CellCoordinate(1, -1) to Cell(Player.O, turn = 1),
                CellCoordinate(2, -1) to Cell(Player.O, turn = 1),

                CellCoordinate(-1, 2) to Cell(Player.X, turn = 2, focussed = true),
                CellCoordinate(0, 2) to Cell(Player.X, turn = 2, focussed = true),
            ),
            board.cells,
        )
    }
}
