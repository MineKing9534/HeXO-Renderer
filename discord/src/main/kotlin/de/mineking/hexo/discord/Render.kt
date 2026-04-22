package de.mineking.hexo.discord

import de.mineking.hexo.core.Board
import de.mineking.hexo.render.render
import net.dv8tion.jda.api.utils.FileUpload
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun Board.render(index: Int): FileUpload {
    val image = render(
        layoutRadius = 64.0,
        gap = 4.0,
        borderThickness = 2f,
        padding = 32,
    )

    val output = ByteArrayOutputStream()
    ImageIO.write(image, "png", output)
    return FileUpload.fromData(output.toByteArray(), "hexo_board_$index.png")
}
