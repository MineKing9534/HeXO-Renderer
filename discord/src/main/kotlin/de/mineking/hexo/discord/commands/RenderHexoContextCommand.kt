package de.mineking.hexo.discord.commands

import de.mineking.discord.commands.localizedMessageCommand
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.builder.codeBlock
import de.mineking.discord.ui.builder.components.buildTextDisplay
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.components.textDisplay
import de.mineking.hexo.discord.HeXODiscordBot
import de.mineking.hexo.discord.createHexoRenderResponse
import de.mineking.hexo.discord.finalErrorResponse
import de.mineking.hexo.discord.renderAsComponent
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.utils.messages.MessageCreateData

context(main: HeXODiscordBot)
fun renderHexoContextCommand() = localizedMessageCommand<RenderHexoContextCommandLocalization>("renderMessage") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    execute {
        val segments = target.contentRaw.parseSegments()
        if (segments.isEmpty()) {
            finalErrorResponse(localization.responseError(userLocale))
        }

        reply(segments.render()).queue()
    }
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

interface RenderHexoContextCommandLocalization : LocalizationFile {
    @Localize
    fun responseError(@Locale locale: DiscordLocale): String
}
