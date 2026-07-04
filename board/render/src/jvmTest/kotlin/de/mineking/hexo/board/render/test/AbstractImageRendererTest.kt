package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.parse.parseHTTTXNotation
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.DefaultTheme
import de.mineking.hexo.board.render.image.Theme
import de.mineking.hexo.core.CellOwner
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertTrue

abstract class AbstractImageRendererTest(private val extension: String, private val renderer: BoardRenderer<Theme, ByteArray>) {
    private fun test(name: String, board: Board, theme: DefaultTheme) = runTest {
        val actual = renderer.render(board, theme.theme)
        val expected = javaClass.getResourceAsStream("/$name.${theme.name.lowercase()}.$extension")?.readAllBytes()

        assertTrue(actual.contentEquals(expected))
    }

    @ParameterizedTest
    @EnumSource
    fun `longsword`(theme: DefaultTheme) {
        val board = """
            version[1];
            1. [0,1][1,-1];
            2. [1,0][-1,0];
            3. [2,0][-4,0];
        """.trimIndent().parseHTTTXNotation().copy()

        board[-4, 0].highlight = CellHighlight(null)
        board[-3, 0].highlight = CellHighlight(null)

        test("longsword", board, theme)
    }

    @ParameterizedTest
    @EnumSource
    fun `labels and highlights`(theme: DefaultTheme) {
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

        test("labels", board, theme)
    }

    @ParameterizedTest
    @EnumSource
    fun `line highlight test`(theme: DefaultTheme) {
        val board = MutableBoard()
        board[0, 1].apply {
            owner = CellOwner.X
            label = "a"
        }
        board[0, 2].owner = CellOwner.O
        board.highlightLine(CellCoordinate(1, 0), Direction.Left, length = 4, color = CellOwner.X)
        board.highlightLine(CellCoordinate(0, 2), Direction.TopLeft, length = 5, color = null)

        test("line_highlight", board, theme)
    }
}
