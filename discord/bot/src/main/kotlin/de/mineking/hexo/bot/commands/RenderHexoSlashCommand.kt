package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.SlashCommand
import de.mineking.discord.commands.enumOption
import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.commands.nullableStringOption
import de.mineking.discord.commands.orElse
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.components.modal.textInput
import de.mineking.discord.ui.builder.components.modal.withLocalizedLabel
import de.mineking.discord.ui.getValue
import de.mineking.discord.ui.initialize
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.modal.ModalMenu
import de.mineking.discord.ui.modal.getValue
import de.mineking.discord.ui.modal.replyModal
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedModal
import de.mineking.discord.ui.render
import de.mineking.discord.ui.setValue
import de.mineking.discord.ui.state
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.defaultHexoTheme
import de.mineking.hexo.bot.utils.asMediaGalleryItem
import de.mineking.hexo.bot.utils.finalErrorResponse
import de.mineking.hexo.bot.utils.replyRichHexoNotation
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder

context(main: HeXODiscordBot)
fun renderHexoSlashCommand(): SlashCommand = { parent ->
    val modal = manager.get<UIManager>().renderHexoModal()
    renderHexoCommandImpl(modal)(parent)
}

private data class RenderHexoModalParameter(val event: Interaction, val theme: DefaultTheme)

context(main: HeXODiscordBot)
private fun UIManager.renderHexoModal() = registerLocalizedModal<RenderHexoModalParameter, RenderHexoCommandLocalization>("hexo") {
    var theme by state(DefaultTheme.HDS)
    initialize {
        theme = it.theme
    }

    render {
        val interaction = parameter({ error("") }, { it.event }, { this })
        localize(interaction.userLocale)
    }

    val content by +textInput("input", style = TextInputStyle.PARAGRAPH).withLocalizedLabel()

    execute {
        replyRichHexoNotation(content, theme)
    }
}

context(main: HeXODiscordBot)
private fun renderHexoCommandImpl(
    modal: ModalMenu<RenderHexoModalParameter, *>,
) = localizedSlashCommand<RenderHexoCommandLocalization>("hexo") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val input = nullableStringOption("input")
    val theme = enumOption<DefaultTheme>("theme")
        .orElse { user.defaultHexoTheme() }

    execute {
        val input = input()
        val theme = theme()

        if (input == null) {
            replyModal(modal, RenderHexoModalParameter(event, theme)).queue()
        } else {
            deferReply().queue()
            val board = try {
                main.notationParser.parse(input)
            } catch (e: HexoNotationException) {
                finalErrorResponse(localization.responseError(userLocale, input, e.message))
            }

            hook.editOriginal(
                MessageEditBuilder()
                    .setReplace(true)
                    .setComponents(MediaGallery.of(main.run { board.asMediaGalleryItem(theme) }))
                    .build(),
            ).queue()
        }
    }
}

interface RenderHexoCommandLocalization : LocalizationFile {
    @Localize
    fun responseError(@Locale locale: DiscordLocale, @LocalizationParameter input: String, @LocalizationParameter message: String?): String
}
