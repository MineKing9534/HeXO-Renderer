package de.mineking.hexo.render.test

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Player
import de.mineking.hexo.render.RectilinearNotationType
import de.mineking.hexo.render.renderRectilinearNotation
import kotlin.test.Test
import kotlin.test.assertEquals

class RectilinearNotationRenderTest {
    @Test
    fun `render compact`() {
        val board = Board()
        board[0, 0].owner = Player.X
        board[3, 0].owner = Player.X
        board[0, 1].owner = Player.O
        board[2, 1].owner = Player.O
        board[0, 3].owner = Player.X

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        assertEquals(
            "x-x/o.o//x",
            rendered,
        )
    }

    @Test
    fun `render compact minimize prefix single line`() {
        val board = Board()
        board[10, 0].owner = Player.X

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        assertEquals("x", rendered)
    }

    @Test
    fun `render compact minimize prefix multiple lines`() {
        val board = Board()
        board[10, 0].owner = Player.X
        board[10, 1].owner = Player.O

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        assertEquals("x/o", rendered)
    }

    @Test
    fun `render compact long empty space`() {
        val board = Board()
        board[10, 0].owner = Player.X
        board[0, 1].owner = Player.O

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        assertEquals("10x/o", rendered)
    }

    @Test
    fun `render multiline`() {
        val board = Board()
        board[0, 0].owner = Player.X
        board[3, 0].owner = Player.X
        board[0, 1].owner = Player.O
        board[2, 1].owner = Player.O
        board[0, 3].owner = Player.X

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Multiline)
        assertEquals(
            """
                x . . x
                 o . o
                
                   x
            """.trimIndent(),
            rendered,
        )
    }
}
