package de.mineking.hexo.render.test

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Player
import de.mineking.hexo.render.ImageBoardRenderer
import de.mineking.hexo.render.renderToByteArray
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class ImageRenderTest {
    @Test
    fun `render image`() = runTest {
        val board = Board()
        board[0, 0].owner = Player.X
        board[3, 0].owner = Player.X
        board[0, 1].owner = Player.O
        board[2, 1].owner = Player.O
        board[0, 3].owner = Player.X

        val rendered = ImageBoardRenderer.run { board.renderToByteArray() }
        val expected = javaClass.getResourceAsStream("/grid.png")?.readAllBytes()

        assertContentEquals(expected, rendered)
    }
}
