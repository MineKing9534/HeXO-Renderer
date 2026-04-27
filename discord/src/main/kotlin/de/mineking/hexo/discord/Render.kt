package de.mineking.hexo.discord

import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.builder.codeBlock
import de.mineking.discord.ui.builder.components.buildTextDisplay
import de.mineking.discord.ui.builder.components.message.mediaGallery
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.components.textDisplay
import de.mineking.discord.ui.message.MessageComponent
import de.mineking.hexo.core.Board
import de.mineking.hexo.render.renderToByteArray
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData

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

context(event: IReplyCallback, main: HeXODiscordBot)
suspend fun String.renderRichHexoNotation(): MessageCreateData {
    val segments = parseSegments()
    if (segments.isEmpty()) {
        event.respond(MessageColor.Error, main.localization<RenderLocalization>().responseError(event.userLocale))
    }

    return segments.render()
}

interface RenderLocalization : LocalizationFile {
    @Localize
    fun responseError(@Locale locale: DiscordLocale): String
}

context(main: HeXODiscordBot)
private suspend fun List<Segment>.render(): MessageCreateData {
    val renderedSegments = hashSetOf<Int>()
    return createHexoRenderResponse(this) { index, segment ->
        fun appendSpacerIfNecessary() {
            if (index - 1 in renderedSegments) {
                +separator(invisible = true, spacing = Separator.Spacing.LARGE)
            }
        }

        when (segment) {
            is Segment.Text -> {
                appendSpacerIfNecessary()
                +textDisplay(segment.content)
            }
            is Segment.Code -> try {
                +main.notationParser.parse(segment.content).renderAsComponent(index)
                renderedSegments += index
            } catch (_: IllegalArgumentException) {
                appendSpacerIfNecessary()
                +buildTextDisplay {
                    +codeBlock(segment.language ?: "", segment.content)
                }
            }
        }
    }
}

private sealed interface Segment {
    data class Text(val content: String) : Segment
    data class Code(val content: String, val language: String?) : Segment
}

private const val FENCE = "```"

private fun String.parseSegments(): List<Segment> {
    if (isBlank()) return emptyList()

    val out = mutableListOf<Segment>()
    var pos = 0

    while (true) {
        val start = indexOf(FENCE, pos)
        if (start == -1) {
            substring(pos).takeIf { it.isNotBlank() }?.let {
                out += Segment.Text(it)
            }
            return out
        }

        substring(pos, start).takeIf { it.isNotBlank() }?.let {
            out += Segment.Text(it)
        }

        val openEnd = start + FENCE.length
        val close = indexOf(FENCE, openEnd)
        if (close == -1) {
            out += Segment.Text(substring(start))
            return out
        }

        val newline = indexOf('\n', openEnd).takeIf { it != -1 && it < close }
        val (code, lang) =
            if (newline == null) {
                substring(openEnd, close) to null
            } else {
                val content = substring(newline + 1, close)
                val lang = substring(openEnd, newline).ifBlank { null }
                content to lang
            }

        out += Segment.Code(code, lang)
        pos = close + FENCE.length
    }
}
