package de.mineking.hexo.render.test

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Player
import de.mineking.hexo.render.renderImageToByteArray
import kotlin.test.Test
import kotlin.test.assertTrue

class ImageRenderTest {
    @Test
    fun `render image`() {
        val board = Board()
        board[0, 0].apply {
            owner = Player.X
            highlighted = true
        }
        board[3, 0].apply {
            owner = Player.X
            focussed = true
        }
        board[0, 1].owner = Player.O
        board[2, 1].owner = Player.O
        board[0, 3].owner = Player.X

        val rendered = board.renderImageToByteArray()
        val expected = javaClass.getResourceAsStream("/grid.png")?.readAllBytes()

        assertTrue(expected.contentEquals(rendered))
    }
}
