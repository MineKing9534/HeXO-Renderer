package de.mineking.hexo.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.render.BoardRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ImageBoardRenderer(
    private val layoutRadius: Double,
    private val padding: Int,
    private val focusWinningRows: Boolean = true,
    private val theme: Theme = BasicTheme.Default,
) : BoardRenderer<BufferedImage> {
    companion object {
        val Default = ImageBoardRenderer(
            layoutRadius = 64.0,
            padding = 32,
        )
    }

    override suspend fun Board.render() = renderToImage(
        layoutRadius = layoutRadius,
        padding = padding,
        focusWinningRows = focusWinningRows,
        theme = theme,
    )
}

context(renderer: BoardRenderer<BufferedImage>)
suspend fun Board.renderToByteArray() = renderer.run { render() }.toByteArray()

private fun BufferedImage.toByteArray(): ByteArray = ByteArrayOutputStream().apply {
    ImageIO.write(this@toByteArray, "png", this@apply)
}.toByteArray()
