package de.mineking.hexo.discord.commands

import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.commands.requiredStringOption
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.builder.codeBlock
import de.mineking.discord.ui.builder.components.buildTextDisplay
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.mediaGallery
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.hexo.core.Board
import de.mineking.hexo.discord.finalErrorResponse
import de.mineking.hexo.discord.render
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

fun renderHexoSlashCommand() = localizedSlashCommand<RenderHexoSlashCommandLocalization>("render") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val input = requiredStringOption("input")

    execute {
        val input = input()
        val board = try {
            Board.fromRectilinearNotation(input)
        } catch (e: IllegalArgumentException) {
            finalErrorResponse(localization.responseError(userLocale, input, e.message))
        }

        reply(createHexoRenderResponse(listOf(input to board))).queue()
    }
}

fun createHexoRenderResponse(boards: List<Pair<String, Board>>) = MessageCreateBuilder()
    .setComponents(
        container {
            boards.forEachIndexed { index, (input, board) ->
                +buildTextDisplay {
                    +codeBlock("hexo", input)
                }
                +mediaGallery(MediaGalleryItem.fromFile(board.render(index)))

                if (index < boards.lastIndex) {
                    +separator(spacing = Separator.Spacing.LARGE)
                }
            }
        }.render()
    )
    .build()

interface RenderHexoSlashCommandLocalization : LocalizationFile {
    @Localize
    fun responseError(@Locale locale: DiscordLocale, @LocalizationParameter input: String, @LocalizationParameter message: String?): String
}
