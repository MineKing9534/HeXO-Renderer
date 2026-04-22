package de.mineking.hexo.discord.commands

import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.commands.requiredStringOption
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.replyMenu
import de.mineking.hexo.discord.finalErrorResponse
import de.mineking.hexo.discord.menus.GameMenuParameter
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import kotlin.uuid.Uuid

fun gameCommand(gameMenu: MessageMenu<GameMenuParameter, *>) = localizedSlashCommand<GameCommandLocalization>("game") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val id = requiredStringOption("id")

    execute {
        val id = id()
        val parsed = try {
            Uuid.parse(id.split("/").last())
        } catch (_: IllegalArgumentException) {
            finalErrorResponse(localization.responseErrorInvalidId(userLocale, id))
        }

        deferReply().queue()
        replyMenu(gameMenu, GameMenuParameter(event, parsed, Int.MAX_VALUE)).queue()
    }
}

interface GameCommandLocalization : LocalizationFile {
    @Localize
    fun responseErrorInvalidId(@Locale locale: DiscordLocale, @LocalizationParameter id: String): String
}
