package de.mineking.hexo.discord

import de.mineking.hexo.core.Board
import de.mineking.hexo.render.renderToByteArray
import net.dv8tion.jda.api.utils.FileUpload

context(main: HeXODiscordBot)
suspend fun Board.render(index: Int = 0) = FileUpload.fromData(main.boardRenderer.run { renderToByteArray() }, "hexo_board_$index.png")
