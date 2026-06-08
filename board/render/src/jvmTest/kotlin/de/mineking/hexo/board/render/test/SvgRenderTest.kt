package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.render.image.renderToSvg
import de.mineking.hexo.core.CellOwner
import kotlin.test.Test
import kotlin.test.assertEquals

class SvgRenderTest {
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

        val rendered = board.renderToSvg(padding = 32)
        val expected = javaClass.getResourceAsStream("/example.svg")?.bufferedReader()?.readText()

        assertEquals(expected, rendered)
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

        val rendered = board.renderToSvg(padding = 32)
        val expected = javaClass.getResourceAsStream("/highlight_lines.svg")?.bufferedReader()?.readText()

        assertEquals(expected, rendered)
    }
}
