package de.mineking.hexo.discord

import de.mineking.hexo.core.Board
import de.mineking.hexo.render.renderAsImage
import net.dv8tion.jda.api.utils.FileUpload
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun Board.render(index: Int): FileUpload {
    val image = renderAsImage(
        layoutRadius = 64.0,
        gap = 6.0,
        borderThickness = 2f,
        padding = 32,
    )

    val output = ByteArrayOutputStream()
    ImageIO.write(image, "png", output)
    return FileUpload.fromData(output.toByteArray(), "hexo_board_$index.png")
}
