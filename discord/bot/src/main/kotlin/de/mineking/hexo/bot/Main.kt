package de.mineking.hexo.bot

import de.mineking.discord.Manager
import de.mineking.discord.discordToolKit
import de.mineking.discord.localization.DefaultLocalizationManager
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.read
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.utils.await
import de.mineking.discord.utils.listen
import de.mineking.discord.withLocalization
import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.caching.createCachingRepositories
import de.mineking.hexo.bot.commands.accountLinkCommand
import de.mineking.hexo.bot.commands.gameCommand
import de.mineking.hexo.bot.commands.leaderboardCommand
import de.mineking.hexo.bot.commands.profileCommand
import de.mineking.hexo.bot.commands.renderHexoContextCommand
import de.mineking.hexo.bot.commands.renderHexoSlashCommand
import de.mineking.hexo.bot.menus.GameMenuParameter
import de.mineking.hexo.bot.menus.ProfileMenuParameter
import de.mineking.hexo.bot.menus.accountLinkMenu
import de.mineking.hexo.bot.menus.gameMenu
import de.mineking.hexo.bot.menus.leaderboardMenu
import de.mineking.hexo.bot.menus.profileMenu
import de.mineking.hexo.link.AccountLinkRepository
import de.mineking.hexo.link.DiscordUserId
import de.mineking.hexo.link.HexoDatabaseManager
import de.mineking.hexo.parse.RectilinearStateBKETurnNotationParser
import de.mineking.hexo.parse.cached
import de.mineking.hexo.render.ImageBoardRenderer
import de.mineking.hexo.render.cached
import dev.freya02.jda.emojis.unicode.Emojis
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.utils.messages.MessageRequest

internal val logger = KotlinLogging.logger {}

fun main() {
    MessageRequest.setDefaultUseComponentsV2(true)

    val token = System.getenv("TOKEN")
        ?: error("Missing required 'TOKEN' environment variable!")

    val _ = HeXODiscordBot(token)
}

val Manager.main get() = manager.bot as HeXODiscordBot

class HeXODiscordBot(token: String) {
    private val client = HexoApiClient(socketIOOptions = null)
    private val repositories = client.createCachingRepositories()

    private val database = HexoDatabaseManager("jdbc:postgresql://localhost:5432/hexo?user=hexo&password=hexo")

    val jda = JDABuilder.createLight(token)
        .setStatus(OnlineStatus.ONLINE)
        .setActivity(Activity.playing("HeXO"))
        .build()

    val emojiManager = EmojiManager(jda)

    val notationParser = SandboxFormationOrNotationParser(
        formationRepository = repositories.formations,
        delegateParser = RectilinearStateBKETurnNotationParser,
    ).cached()
    val boardRenderer = ImageBoardRenderer.Default.cached()

    lateinit var gameMenu: MessageMenu<GameMenuParameter, *> private set
    lateinit var profileMenu: MessageMenu<ProfileMenuParameter, *> private set
    lateinit var leaderboardMenu: MessageMenu<Interaction, *> private set
    lateinit var accountLinkMenu: MessageMenu<IModalCallback, *> private set

    val dtk = discordToolKit(jda, this)
        .withLocalization<_, DefaultLocalizationManager>()
        .withUIManager {
            localize()
            installErrorHandling()

            gameMenu = gameMenu(repositories.finishedGames)
            profileMenu = profileMenu(repositories.profiles)
            leaderboardMenu = leaderboardMenu(repositories.leaderboard, profileMenu)
            accountLinkMenu = accountLinkMenu(AccountLinkRepository(database), repositories.profiles)
        }
        .withCommandManager {
            localize()
            installErrorHandling()

            +renderHexoContextCommand()
            +renderHexoSlashCommand()

            +gameCommand(gameMenu)
            +leaderboardCommand(leaderboardMenu)
            +profileCommand(profileMenu)

            +accountLinkCommand(accountLinkMenu)

            updateCommands().queue()
        }
        .build()

    init {
        jda.registerMessageDeleteListener()
    }
}

private fun JDA.registerMessageDeleteListener() {
    listen<MessageReactionAddEvent> {
        if (reaction.emoji != Emojis.WASTEBASKET) return@listen

        val message = retrieveMessage().await()
        if (message.author != jda.selfUser) return@listen
        if (message.interactionMetadata?.user?.idLong != userIdLong) return@listen

        message.delete().queue()
    }
}

inline fun <reified L : LocalizationFile> HeXODiscordBot.localization() = dtk.localizationManager.read<L>()
val UserSnowflake.userId get() = DiscordUserId(idLong)
