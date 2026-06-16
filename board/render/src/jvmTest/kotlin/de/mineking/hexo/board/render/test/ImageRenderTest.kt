package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.clone
import de.mineking.hexo.board.parse.parseHTTTXNotation
import de.mineking.hexo.board.render.image.HTTTXTheme
import de.mineking.hexo.board.render.image.renderToImage
import de.mineking.hexo.core.CellOwner
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

class ImageRenderTest {
    @Test
    fun `render image`() {
        val board = MutableBoard()
        board[0, 0].apply {
            owner = CellOwner.X
            highlight = CellHighlight(null)
        }
        board[3, 0].apply {
            owner = CellOwner.X
            focused = true
        }
        board[0, 1].apply {
            owner = CellOwner.O
            label = "1"
        }
        board[2, 1].apply {
            owner = CellOwner.O
            highlight = CellHighlight(CellOwner.X)
        }
        board[0, 3].owner = CellOwner.X
        board[0, 4].apply {
            highlight = CellHighlight(null)
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
        val board = MutableBoard()
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

    @Test
    fun `htttx theme`() {
        val board = """
            version[1];
            1. [0,1][1,-1];
            2. [1,0][-1,0];
            3. [2,0][-4,0];
        """.trimIndent().parseHTTTXNotation().clone()

        board[-4, 0].highlight = CellHighlight(null)
        board[-3, 0].highlight = CellHighlight(null)

        val renderedImage = board.renderToImage(
            layoutRadius = 64.0,
            padding = 32,
            theme = HTTTXTheme,
        )

        val renderedBytes = ByteArrayOutputStream()
        ImageIO.write(renderedImage, "png", renderedBytes)

        val expected = javaClass.getResourceAsStream("/httx.png")?.readAllBytes()

        assertTrue(expected.contentEquals(renderedBytes.toByteArray()))
    }
}
