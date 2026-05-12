package de.mineking.hexo.discord.commands

import de.mineking.discord.commands.localizedMessageCommand
import de.mineking.discord.localization.LocalizationFile
import de.mineking.hexo.discord.HeXODiscordBot
import de.mineking.hexo.discord.replyRichHexoNotation
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

context(main: HeXODiscordBot)
fun renderHexoContextCommand() = localizedMessageCommand<RenderHexoContextCommandLocalization>("renderMessage") {
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    execute {
        replyRichHexoNotation(target.contentRaw)
    }
}

interface RenderHexoContextCommandLocalization : LocalizationFile
