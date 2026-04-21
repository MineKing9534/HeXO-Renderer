package de.mineking.hexo.discord.commands

import de.mineking.discord.commands.localizedMessageCommand
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.Localize
import de.mineking.hexo.core.Board
import de.mineking.hexo.discord.finalErrorResponse
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

fun renderHexoContextCommand() = localizedMessageCommand<RenderHexoContextCommandLocalization>("renderMessage") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    execute {
        val boards = target.contentRaw.findHexoNotations()
        if (boards.isEmpty()) {
            finalErrorResponse(localization.responseError(userLocale))
        }

        reply(createHexoRenderResponse(boards)).queue()
    }
}

private val regex = "(?s)`(?:``)?.*?\\n(.*?)`(?:``)?".toRegex()
private fun String.findHexoNotations() = try {
    val board = Board.fromRectilinearNotation(this)
    listOf(this to board)
} catch (_: IllegalArgumentException) {
    regex.findAll(this).mapNotNull {
        val value = it.groupValues[1]
        try {
            value to Board.fromRectilinearNotation(value)
        } catch (_: IllegalArgumentException) {
            null
        }
    }.toList()
}

interface RenderHexoContextCommandLocalization : LocalizationFile {
    @Localize
    fun responseError(@Locale locale: DiscordLocale): String
}
