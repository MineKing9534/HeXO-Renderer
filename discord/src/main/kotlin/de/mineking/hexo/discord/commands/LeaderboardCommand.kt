package de.mineking.hexo.discord.commands

import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.replyMenu
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionContextType

fun leaderboardCommand(leaderboardMenu: MessageMenu<Interaction, *>) = localizedSlashCommand<LeaderboardCommandLocalization>("leaderboard") {
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    execute {
        deferReply().queue()
        replyMenu(leaderboardMenu, this).queue()
    }
}

interface LeaderboardCommandLocalization : LocalizationFile
