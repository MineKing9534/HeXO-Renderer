package de.mineking.hexo.discord.commands

import de.mineking.discord.commands.SlashCommand
import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.commands.nullableStringOption
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.components.modal.textInput
import de.mineking.discord.ui.builder.components.modal.withLocalizedLabel
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.modal.ModalMenu
import de.mineking.discord.ui.modal.getValue
import de.mineking.discord.ui.modal.replyModal
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedModal
import de.mineking.discord.ui.render
import de.mineking.hexo.discord.HeXODiscordBot
import de.mineking.hexo.discord.createHexoRenderResponse
import de.mineking.hexo.discord.finalErrorResponse
import de.mineking.hexo.discord.renderAsComponent
import de.mineking.hexo.discord.renderRichHexoNotation
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.utils.messages.MessageEditData

context(main: HeXODiscordBot)
fun renderHexoSlashCommand(): SlashCommand = { parent ->
    val modal = manager.get<UIManager>().renderHexoModal()
    renderHexoCommandImpl(modal)(parent)
}

context(main: HeXODiscordBot)
private fun UIManager.renderHexoModal() = registerLocalizedModal<Interaction, RenderHexoSlashCommandLocalization>("hexo") {
    render {
        val interaction = parameter({ error("") }, { it }, { this })
        localize(interaction.userLocale)
    }

    val content by +textInput("input", style = TextInputStyle.PARAGRAPH).withLocalizedLabel()

    execute {
        deferReply().queue()
        hook.editOriginal(MessageEditData.fromCreateData(content.renderRichHexoNotation())).queue()
    }
}

context(main: HeXODiscordBot)
private fun renderHexoCommandImpl(
    modal: ModalMenu<Interaction, *>,
) = localizedSlashCommand<RenderHexoSlashCommandLocalization>("hexo") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val input = nullableStringOption("input")

    execute {
        val input = input()
        if (input == null) {
            replyModal(modal, event).queue()
        } else {
            deferReply().queue()
            val board = try {
                main.notationParser.parse(input)
            } catch (e: IllegalArgumentException) {
                finalErrorResponse(localization.responseError(userLocale, input, e.message))
            }

            hook.editOriginal(
                MessageEditData.fromCreateData(
                    createHexoRenderResponse(listOf(board)) { _, board ->
                        +main.boardRenderer.run { board.renderAsComponent() }
                    },
                ),
            ).queue()
        }
    }
}

interface RenderHexoSlashCommandLocalization : LocalizationFile {
    @Localize
    fun responseError(@Locale locale: DiscordLocale, @LocalizationParameter input: String, @LocalizationParameter message: String?): String
}
