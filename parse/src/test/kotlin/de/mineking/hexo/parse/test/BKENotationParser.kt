package de.mineking.hexo.parse.test

import de.mineking.hexo.core.Cell
import de.mineking.hexo.core.CellCoordinate
import de.mineking.hexo.core.Direction
import de.mineking.hexo.core.Player
import de.mineking.hexo.parse.Chirality
import de.mineking.hexo.parse.parseBKENotation
import kotlin.test.Test
import kotlin.test.assertEquals

class BKENotationParser {
    @Test
    fun `parse long sword`() {
        val board = "o A0 A2 x A1 A4 o B1.0 D4.0".parseBKENotation(zeroOffsetLine = Direction.TopRight)
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(Player.X, turn = 0),

                CellCoordinate(0, 1) to Cell(Player.O, turn = 1),
                CellCoordinate(1, -1) to Cell(Player.O, turn = 1),

                CellCoordinate(1, 0) to Cell(Player.X, turn = 2),
                CellCoordinate(-1, 0) to Cell(Player.X, turn = 2),

                CellCoordinate(2, 0) to Cell(Player.O, turn = 3, focussed = true),
                CellCoordinate(-4, 0) to Cell(Player.O, turn = 3, focussed = true),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse x A0 A1 o A3 A4 x B0 B1`() {
        val board = "x A0 A1 o A3 A4 x B0 B1".parseBKENotation(zeroOffsetLine = Direction.BottomLeft, chirality = Chirality.CounterClockwise)
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(Player.X, turn = 0),

                CellCoordinate(-1, 1) to Cell(Player.X, turn = 1),
                CellCoordinate(0, 1) to Cell(Player.X, turn = 1),

                CellCoordinate(0, -1) to Cell(Player.O, turn = 2),
                CellCoordinate(1, -1) to Cell(Player.O, turn = 2),

                CellCoordinate(-2, 2) to Cell(Player.X, turn = 3, focussed = true),
                CellCoordinate(-1, 2) to Cell(Player.X, turn = 3, focussed = true),
            ),
            board.cells,
        )
    }
}
