package de.mineking.hexo.render.test

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Player
import de.mineking.hexo.parse.RectilinearNotationParser
import de.mineking.hexo.render.RectilinearNotationType
import de.mineking.hexo.render.renderRectilinearNotation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IntegrationTest {
    private suspend fun integrationTest(type: RectilinearNotationType) {
        val board = Board()
        board[0, 0].owner = Player.X
        board[3, 0].owner = Player.X
        board[0, 1].owner = Player.O
        board[2, 1].owner = Player.O
        board[0, 3].owner = Player.X

        val rendered = board.renderRectilinearNotation(type)
        val parsed = RectilinearNotationParser.parse(rendered)

        assertEquals(board, parsed)
    }

    @Test
    fun `compact integration test`() = runTest {
        integrationTest(RectilinearNotationType.Compact)
    }

    @Test
    fun `multiline integration test`() = runTest {
        integrationTest(RectilinearNotationType.Multiline)
    }

    @Test
    fun `compact with highlight`() = runTest {
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
        val parsed = RectilinearNotationParser.parse(rendered)

        assertEquals(board, parsed)
    }
}
