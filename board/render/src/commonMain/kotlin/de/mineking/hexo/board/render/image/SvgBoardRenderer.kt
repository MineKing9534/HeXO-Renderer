package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.BoardRenderer

class SvgBoardRenderer(
    private val padding: Int,
    private val layoutRadius: Double = 64.0,
    private val theme: Theme = BasicTheme.Default,
    private val prettyPrint: Boolean = false,
) : BoardRenderer<String> {
    companion object {
        val Default = SvgBoardRenderer(padding = 32)
    }

    override suspend fun Board.render() = renderToSvg(padding, layoutRadius, prettyPrint, theme)
}
