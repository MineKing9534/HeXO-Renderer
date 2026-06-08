package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.BoardRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class BufferedImageBoardRenderer(
    private val layoutRadius: Double,
    private val padding: Int,
    private val theme: Theme = BasicTheme.Default,
) : BoardRenderer<BufferedImage> {
    companion object {
        val Default = BufferedImageBoardRenderer(
            layoutRadius = 64.0,
            padding = 32,
        )
    }

    override suspend fun Board.render() = renderToImage(
        layoutRadius = layoutRadius,
        padding = padding,
        theme = theme,
    )
}

fun BoardRenderer<BufferedImage>.outputPngBytes() = object : BoardRenderer<ByteArray> {
    override suspend fun Board.render() = this@outputPngBytes.run { render() }.toPngBytes()
}

context(renderer: BoardRenderer<BufferedImage>)
suspend fun Board.renderPngBytes() = renderer.run { render() }.toPngBytes()

private fun BufferedImage.toPngBytes(): ByteArray = ByteArrayOutputStream().apply {
    ImageIO.write(this@toPngBytes, "png", this@apply)
}.toByteArray()
