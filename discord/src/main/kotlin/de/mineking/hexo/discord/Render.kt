package de.mineking.hexo.discord

import de.mineking.discord.ui.builder.components.message.mediaGallery
import de.mineking.hexo.core.Board
import de.mineking.hexo.render.renderToByteArray
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.utils.FileUpload

context(main: HeXODiscordBot)
suspend fun Board.renderAsComponent(index: Int = 0) = mediaGallery(
    MediaGalleryItem.fromFile(
        FileUpload.fromData(main.boardRenderer.run { renderToByteArray() }, "hexo_board_$index.png"),
    ),
)
