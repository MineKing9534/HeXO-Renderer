package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.localizedUserCommand
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.replyMenu
import de.mineking.hexo.bot.menus.ProfileMenuParameter
import de.mineking.hexo.bot.userId
import de.mineking.hexo.bot.utils.finalErrorResponse
import de.mineking.hexo.link.AccountLinkRepository
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

fun profileUserCommand(
    accountLinkRepository: AccountLinkRepository,
    profileMenu: MessageMenu<ProfileMenuParameter, *>,
) = localizedUserCommand<ProfileCommandLocalization>("viewProfile") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    execute {
        deferReply().queue()

        val profileId = accountLinkRepository.getHexoProfile(target.userId)
            ?: finalErrorResponse(localization.responseErrorNotFoundUser(userLocale, target))

        replyMenu(profileMenu, ProfileMenuParameter(event, profileId)).queue()
    }
}
