package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.replyEventMenu
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback

fun accountLinkCommand(menu: MessageMenu<IModalCallback, *>) = localizedSlashCommand<AccountLinkCommandLocalization>("link") {
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    execute {
        deferReply(true).queue()
        replyEventMenu(menu).queue()
    }
}

interface AccountLinkCommandLocalization : LocalizationFile
