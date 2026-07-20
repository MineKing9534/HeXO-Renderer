import de.mineking.hexo.board.parse.parseRectilinearNotation
import de.mineking.hexo.board.render.image.renderToImage
import java.io.File
import javax.imageio.ImageIO

fun create(input: String, filename: String) {
    val board = input.parseRectilinearNotation()
    val image = board.renderToImage(layoutRadius = 64.0, padding = 32)

    ImageIO.write(image, "png", File(filename))
}

fun main() {
    create(".[N].[U].[M].[B].[E].[R]2[B].[A].[S].[H].[E].[R]", "numberbasher.png")
    create(".[#FF69B4 meow]xxxxxxxxxxxxxxxxx/////x", "meow.png")
    create(".[#FF69B4 meeeeeeeeeeeeeeeeeoooow]", "meeow.png")
}
