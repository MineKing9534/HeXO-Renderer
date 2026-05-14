package de.mineking.hexo.bot.menus

import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.components.localizedTextDisplay
import de.mineking.discord.ui.builder.components.message.ButtonColor
import de.mineking.discord.ui.builder.components.message.button
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.section
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.components.modal.requiredCheckbox
import de.mineking.discord.ui.builder.components.modal.textInput
import de.mineking.discord.ui.builder.components.modal.withLocalizedLabel
import de.mineking.discord.ui.lazy
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.message.modal
import de.mineking.discord.ui.modal.getValue
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedMenu
import de.mineking.discord.ui.renderValue
import de.mineking.discord.ui.terminateRender
import de.mineking.hexo.api.profile.ProfileId
import de.mineking.hexo.api.profile.ProfileRepository
import de.mineking.hexo.bot.CustomEmoji
import de.mineking.hexo.bot.MessageColor
import de.mineking.hexo.bot.main
import de.mineking.hexo.bot.respond
import de.mineking.hexo.bot.userId
import de.mineking.hexo.link.AccountLinkRepository
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback

private val IMAGE_URL_PATTERN = """https://cdn\.discordapp\.com/avatars/(\d+)/.*\.(?:png|jpg|webp|gif)(?:\?size=\d+)?""".trimMargin().toRegex()

fun UIManager.accountLinkMenu(
    linkRepository: AccountLinkRepository,
    profileRepository: ProfileRepository,
) = registerLocalizedMenu<IModalCallback, AccountLinkMenuLocalization>("link") root@{ localization ->
    val event = parameter({ null }, { it }, { event })
    val locale = event?.userLocale

    localize(locale ?: DiscordLocale.UNKNOWN)

    val linkedProfileId by lazy { event?.let { linkRepository.getHexoProfile(event.user.userId) } }
    val profile = renderValue {
        linkedProfileId?.let { profileRepository.getProfile(it) }
    }

    localize(locale ?: DiscordLocale.UNKNOWN) {
        bindParameter("profile", profile)
    }

    val linkModal = modal("link") {
        +localizedTextDisplay("explanation")
        val profileUrl by +textInput("url").withLocalizedLabel()

        execute {
            deferEdit().queue()

            val profileId = ProfileId(profileUrl.split("/").last())
            val profile = profileRepository.getProfile(profileId)
            if (profile == null) {
                respond(MessageColor.Error, localization.responseErrorProfileNotFound(userLocale, profileId), forceNew = true)
                terminateRender()
            }

            val match = profile.image?.let { IMAGE_URL_PATTERN.matchEntire(it) }
            if (match?.groupValues[1] != user.id) {
                respond(MessageColor.Error, localization.responseErrorProfileVerifyFailed(userLocale), forceNew = true)
                terminateRender()
            }

            linkRepository.createLink(user.userId, profile.id)
            switchMenu(this@root.menu)
        }
    }

    val confirmModal = modal("remove") {
        +requiredCheckbox("confirm", description = null).withLocalizedLabel()

        execute {
            linkRepository.removeLinkedProfile(user.userId)
            switchMenu(this@root.menu)
        }
    }

    +container {
        +localizedTextDisplay("title")
        +separator()
        +section(
            accessory = button(
                "link",
                emoji = if (profile == null) main.emojiManager[CustomEmoji.Link] else main.emojiManager[CustomEmoji.Unlink],
                color = if (profile == null) ButtonColor.GREEN else ButtonColor.RED,
            ) {
                switchMenu(if (linkedProfileId == null) linkModal else confirmModal)
            },
            localizedTextDisplay("status"),
        )
    }
}

interface AccountLinkMenuLocalization : LocalizationFile {
    @Localize
    fun responseErrorProfileNotFound(@Locale locale: DiscordLocale, @LocalizationParameter id: ProfileId): String

    @Localize
    fun responseErrorProfileVerifyFailed(@Locale locale: DiscordLocale): String
}
