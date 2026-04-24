package de.mineking.hexo.discord.commands

import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.commands.requiredStringOption
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.builder.components.message.ContainerBuilder
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.hexo.discord.HeXODiscordBot
import de.mineking.hexo.discord.finalErrorResponse
import de.mineking.hexo.discord.renderAsComponent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

context(main: HeXODiscordBot)
fun renderHexoSlashCommand() = localizedSlashCommand<RenderHexoSlashCommandLocalization>("hexo") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val input = requiredStringOption("input")

    execute {
        val input = input()
        val board = try {
            main.rectilinearParser.parse(input)
        } catch (e: IllegalArgumentException) {
            finalErrorResponse(localization.responseError(userLocale, input, e.message))
        }

        reply(createHexoRenderResponse(listOf(board)) { _, board ->
            +main.boardRenderer.run { board.renderAsComponent() }
        }).queue()
    }
}

inline fun <T> createHexoRenderResponse(
    boards: List<T>,
    render: ContainerBuilder.(Int, T) -> Unit,
) = MessageCreateBuilder()
    .setComponents(
        container {
            boards.forEachIndexed { index, item ->
                render(index, item)
            }
        }.renderAsComponent(),
    )
    .build()

interface RenderHexoSlashCommandLocalization : LocalizationFile {
    @Localize
    fun responseError(@Locale locale: DiscordLocale, @LocalizationParameter input: String, @LocalizationParameter message: String?): String
}
