package de.mineking.hexo.discord.commands

import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.commands.requiredStringOption
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.replyMenu
import de.mineking.hexo.api.profile.ProfileId
import de.mineking.hexo.discord.menus.ProfileMenuParameter
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

fun profileCommand(profileMenu: MessageMenu<ProfileMenuParameter, *>) = localizedSlashCommand<ProfileCommandLocalization>("profile") {
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val id = requiredStringOption("id")

    execute {
        val id = id().split("/").last()

        deferReply().queue()
        replyMenu(profileMenu, ProfileMenuParameter(event, ProfileId(id))).queue()
    }
}

interface ProfileCommandLocalization : LocalizationFile
