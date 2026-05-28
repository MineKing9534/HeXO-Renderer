package de.mineking.hexo.render.test

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.Direction
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.parse.parseBKENotation
import de.mineking.hexo.parse.parseRectilinearNotation
import de.mineking.hexo.parse.parseRectilinearStateBKETurnNotation
import de.mineking.hexo.render.RectilinearNotationType
import de.mineking.hexo.render.renderRectilinearNotation
import de.mineking.hexo.render.renderRectilinearStateBKETurnNotation
import kotlin.test.Test
import kotlin.test.assertEquals

class IntegrationTest {
    private fun integrationTest(type: RectilinearNotationType) {
        val board = Board()
        board[0, 0].owner = CellOwner.X
        board[3, 0].owner = CellOwner.X
        board[0, 1].owner = CellOwner.O
        board[2, 1].owner = CellOwner.O
        board[0, 3].owner = CellOwner.X

        val rendered = board.renderRectilinearNotation(type)
        val parsed = rendered.parseRectilinearNotation()

        assertEquals(board, parsed)
    }

    @Test
    fun `compact integration test`() {
        integrationTest(RectilinearNotationType.Compact)
    }

    @Test
    fun `multiline integration test`() {
        integrationTest(RectilinearNotationType.Multiline)
    }

    @Test
    fun `compact with highlight`() {
        val board = Board()
        board[0, 0].apply {
            owner = CellOwner.X
            highlight = CellHighlight(null)
        }
        board[1, 0].highlight = CellHighlight(null)
        board[3, 0].owner = CellOwner.X
        board[0, 1].owner = CellOwner.O
        board[2, 1].owner = CellOwner.O
        board[0, 3].owner = CellOwner.X

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        val parsed = rendered.parseRectilinearNotation()

        assertEquals(board, parsed)
    }

    @Test
    fun `label integration test`() {
        val board = Board()
        board[0, 0].apply {
            owner = CellOwner.X
            label = "a"
        }
        board[1, 0].label = "b"

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        val parsed = rendered.parseRectilinearNotation()

        assertEquals(board, parsed)
    }

    @Test
    fun `bke test`() {
        val input = "o A0 A2 x A1 A4 o B1.0 B4.0"
        val parsed = input.parseBKENotation(null, Direction.Right)
        val rendered = parsed.renderRectilinearStateBKETurnNotation(RectilinearNotationType.Compact)

        assertEquals(input, rendered)
    }

    @Test
    fun `rectilinear state bke turn test`() {
        val input = ".x/xx, d o A0 B1 x B2.0 B2.1"
        val parsed = input.parseRectilinearStateBKETurnNotation()
        val rendered = parsed.renderRectilinearStateBKETurnNotation(RectilinearNotationType.Compact)

        assertEquals(".x/xx, b @(1, 0) o A0 A1 x B3.1 B4.0", rendered)
        val _ = rendered.parseRectilinearStateBKETurnNotation()
    }

    @Test
    fun `rectilinear state bke turn preserves shared offset`() {
        val board = Board()
        board[4, 0].owner = CellOwner.X
        board[0, 0].apply {
            owner = CellOwner.O
            turn = 1
        }

        val rendered = board.renderRectilinearStateBKETurnNotation(RectilinearNotationType.Compact)

        assertEquals("x, > @(-5, 0) o A0", rendered)
    }

    @Test
    fun `bke origin is not placed on a bke move`() {
        val board = Board()
        board[0, 0].owner = CellOwner.X
        board[2, 0].apply {
            owner = CellOwner.O
            turn = 1
            focused = true
        }
        board[1, 0].apply {
            owner = CellOwner.O
            turn = 1
            focused = true
        }

        val rendered = board.renderRectilinearStateBKETurnNotation(RectilinearNotationType.Compact)

        assertEquals("x, q @(2, -1) o A0 A1", rendered)
        assertEquals(board, rendered.parseRectilinearStateBKETurnNotation())
    }
}
