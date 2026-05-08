package de.mineking.hexo.render.test

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.Direction
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.render.renderToImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

class ImageRenderTest {
    @Test
    fun `render image`() {
        val board = Board()
        board[0, 0].apply {
            owner = CellOwner.X
            highlighted = true
        }
        board[3, 0].apply {
            owner = CellOwner.X
            focussed = true
        }
        board[0, 1].apply {
            owner = CellOwner.O
            label = "1"
        }
        board[2, 1].owner = CellOwner.O
        board[0, 3].owner = CellOwner.X
        board[0, 4].apply {
            highlighted = true
            label = "b"
        }
        board[-1, 4].label = "a"

        val renderedImage = board.renderToImage(
            layoutRadius = 64.0,
            padding = 32,
        )

        val renderedBytes = ByteArrayOutputStream()
        ImageIO.write(renderedImage, "png", renderedBytes)

        val expected = javaClass.getResourceAsStream("/example.png")?.readAllBytes()

        assertTrue(expected.contentEquals(renderedBytes.toByteArray()))
    }

    @Test
    fun `highlighted line`() {
        val board = Board()
        board[0, 1].apply {
            owner = CellOwner.X
            label = "a"
        }
        board[0, 2].owner = CellOwner.O
        board.highlightLine(CellCoordinate(1, 0), Direction.Left, length = 4, color = CellOwner.X)
        board.highlightLine(CellCoordinate(0, 2), Direction.TopLeft, length = 5, color = null)

        val renderedImage = board.renderToImage(
            layoutRadius = 64.0,
            padding = 32,
        )

        val renderedBytes = ByteArrayOutputStream()
        ImageIO.write(renderedImage, "png", renderedBytes)

        val expected = javaClass.getResourceAsStream("/highlight_lines.png")?.readAllBytes()

        assertTrue(expected.contentEquals(renderedBytes.toByteArray()))
    }
}
