package de.mineking.hexo.discord

import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.Localize
import de.mineking.hexo.board.Board
import de.mineking.hexo.render.renderToByteArray
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.utils.FileUpload
import kotlin.uuid.Uuid

context(main: HeXODiscordBot)
suspend fun Board.asMediaGalleryItem() = MediaGalleryItem.fromFile(
    FileUpload.fromData(main.boardRenderer.run { renderToByteArray() }, "${Uuid.random()}.png"),
)

context(main: HeXODiscordBot)
suspend fun IReplyCallback.replyRichHexoNotation(content: String) {
    deferReply().queue()

    val components = content.renderToComponents().layout()
    if (components.filterIsInstance<MediaGallery>().isEmpty()) {
        val errorMessage = renderAsComponent(MessageColor.Error, main.localization<RenderLocalization>().responseError(userLocale))
        hook.editOriginalComponents(errorMessage).useComponentsV2().queue()
    } else {
        hook.editOriginalComponents(components).useComponentsV2().queue()
    }
}

interface RenderLocalization : LocalizationFile {
    @Localize
    fun responseError(@Locale locale: DiscordLocale): String
}

private fun List<MessageTopLevelComponent>.layout(): List<MessageTopLevelComponent> = fold(mutableListOf()) { result, current ->
    val last = result.lastOrNull()
    when (current) {
        is TextDisplay if last is TextDisplay -> {
            result[result.lastIndex] = TextDisplay.of(last.content + current.content)
        }
        is MediaGallery if last is MediaGallery -> {
            result += Separator.createDivider(Separator.Spacing.LARGE)
            result += current
        }
        else -> result += current
    }

    result
}

private class ComponentParserState {
    val result = mutableListOf<MessageTopLevelComponent>()
    val afterParagraph = mutableListOf<MediaGalleryItem>()
    val temp = StringBuilder()
    var position = 0

    fun flush() {
        if (temp.isNotBlank()) {
            result += TextDisplay.of(temp.toString())
            temp.clear()
        }

        if (afterParagraph.isNotEmpty()) {
            result += MediaGallery.of(afterParagraph)
            afterParagraph.clear()
        }
    }
}

context(main: HeXODiscordBot)
private suspend fun String.renderToComponents(): List<MessageTopLevelComponent> {
    if (isBlank()) return emptyList()
    val state = ComponentParserState()

    while (state.position < length) {
        val type = SegmentParser.entries.firstOrNull {
            startsWith(it.symbol, state.position)
        }

        state.handle(this, type)
    }

    state.flush()
    return state.result
}

context(main: HeXODiscordBot)
private suspend fun ComponentParserState.handle(str: String, type: SegmentParser?) {
    if (type == null) {
        val c = str[position]
        temp.append(c)

        if (c == '\n') flush()
        position++
    } else {
        val start = position + type.symbol.length
        val end = str.indexOf(type.symbol, start)

        if (end == -1) {
            temp.append(str[position++])
        } else {
            val content = str.substring(start, end)

            if (type.keepAsText) {
                temp.append("${type.symbol}$content${type.symbol}")
            } else {
                flush()
            }

            type.handle(content, this)
            position = end + type.symbol.length
        }
    }
}

private enum class SegmentParser(val symbol: String, val keepAsText: Boolean) {
    CodeBlock("```", keepAsText = false) {
        context(main: HeXODiscordBot)
        override suspend fun handle(content: String, state: ComponentParserState) {
            val (code, lang) = content.decodeCodeAndLanguage()
            state.result += try {
                MediaGallery.of(main.notationParser.parse(code).asMediaGalleryItem())
            } catch (_: IllegalArgumentException) {
                TextDisplay.of("$symbol${lang?.let { "$it\n" } ?: ""}$code$symbol")
            }
        }

        private fun String.decodeCodeAndLanguage(): Pair<String, String?> {
            val newline = indexOf('\n')
            return if (newline == -1) {
                this to null
            } else {
                val code = substring(newline + 1)
                val lang = substring(0, newline).ifBlank { null }
                code to lang
            }
        }
    },
    Code("`", keepAsText = true) {
        context(main: HeXODiscordBot)
        override suspend fun handle(content: String, state: ComponentParserState) {
            try {
                state.afterParagraph += main.notationParser.parse(content).asMediaGalleryItem()
            } catch (_: IllegalArgumentException) {
            }
        }
    },
    ;

    context(main: HeXODiscordBot)
    abstract suspend fun handle(content: String, state: ComponentParserState)
}
