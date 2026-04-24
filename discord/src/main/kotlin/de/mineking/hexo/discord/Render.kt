package de.mineking.hexo.discord

import de.mineking.discord.ui.builder.components.message.mediaGallery
import de.mineking.discord.ui.message.MessageComponent
import de.mineking.hexo.core.Board
import de.mineking.hexo.render.renderToByteArray
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

context(main: HeXODiscordBot)
suspend fun Board.renderAsComponent(index: Int = 0) = mediaGallery(
    MediaGalleryItem.fromFile(
        FileUpload.fromData(main.boardRenderer.run { renderToByteArray() }, "hexo_board_$index.png"),
    ),
)

class MessageBuilder {
    val components: List<MessageComponent<out MessageTopLevelComponent>>
        field = mutableListOf<MessageComponent<out MessageTopLevelComponent>>()

    operator fun MessageComponent<out MessageTopLevelComponent>.unaryPlus() {
        components += this
    }
}

inline fun <T> createHexoRenderResponse(
    boards: List<T>,
    render: MessageBuilder.(Int, T) -> Unit,
) = MessageCreateBuilder()
    .setComponents(
        MessageBuilder().apply {
            boards.forEachIndexed { index, item ->
                render(index, item)
            }
        }.components.flatMap { it.renderAsComponent() },
    )
    .build()
