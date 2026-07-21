package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.render.image.BufferedImageBoardRenderer
import de.mineking.hexo.board.render.image.outputPngBytes
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class PngRenderTest : AbstractImageRendererTest("png", BufferedImageBoardRenderer.Default.outputPngBytes()) {
    @EnumSource
    @ParameterizedTest
    fun `long label test`(theme: DefaultTheme) {
        val board = MutableBoard().apply { this[0, 0].label = "meow meow" }
        test("long_label", board, theme)
    }
}
