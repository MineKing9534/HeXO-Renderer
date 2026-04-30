package de.mineking.hexo.render.test

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Player
import de.mineking.hexo.parse.parseRectilinearNotation
import de.mineking.hexo.render.RectilinearNotationType
import de.mineking.hexo.render.renderRectilinearNotation
import kotlin.test.Test
import kotlin.test.assertEquals

class IntegrationTest {
    private fun integrationTest(type: RectilinearNotationType) {
        val board = Board()
        board[0, 0].owner = Player.X
        board[3, 0].owner = Player.X
        board[0, 1].owner = Player.O
        board[2, 1].owner = Player.O
        board[0, 3].owner = Player.X

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
            owner = Player.X
            highlighted = true
        }
        board[1, 0].highlighted = true
        board[3, 0].owner = Player.X
        board[0, 1].owner = Player.O
        board[2, 1].owner = Player.O
        board[0, 3].owner = Player.X

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        val parsed = rendered.parseRectilinearNotation()

        assertEquals(board, parsed)
    }

    @Test
    fun `label integration test`() {
        val board = Board()
        board[0, 0].apply {
            owner = Player.X
            label = "a"
        }
        board[1, 0].label = "b"

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        val parsed = rendered.parseRectilinearNotation()

        assertEquals(board, parsed)
    }
}
