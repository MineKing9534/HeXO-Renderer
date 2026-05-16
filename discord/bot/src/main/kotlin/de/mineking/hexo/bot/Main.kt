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
import de.mineking.hexo.api.HexoRepositories
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
import de.mineking.hexo.bot.utils.LinkedRolesUpdateService
import de.mineking.hexo.bot.utils.SandboxFormationOrNotationParser
import de.mineking.hexo.bot.utils.installErrorHandling
import de.mineking.hexo.bot.utils.updateLinkedRoleMetadata
import de.mineking.hexo.link.AccountLinkRepository
import de.mineking.hexo.link.DiscordUserId
import de.mineking.hexo.link.HexoDatabaseManager
import de.mineking.hexo.link.oauth2.DiscordOAuth2Client
import de.mineking.hexo.link.oauth2.DiscordUserAuthenticationRepository
import de.mineking.hexo.parse.RectilinearStateBKETurnNotationParser
import de.mineking.hexo.parse.cached
import de.mineking.hexo.render.ImageBoardRenderer
import de.mineking.hexo.render.cached
import dev.freya02.jda.emojis.unicode.Emojis
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
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

    val config = Config.fromEnvironment()

    val client = HexoApiClient(socketIOOptions = null)
    val repositories = client.createCachingRepositories()

    val database = config.database?.let { HexoDatabaseManager(it.url) }
    val discordOAuth2Client = when {
        config.oauth2 != null && config.server != null ->
            DiscordOAuth2Client(config.oauth2.clientId, config.oauth2.clientSecret, "${config.server.url}/oauth2/callback")
        else -> null
    }

    val discordUserAuthenticationRepository = when {
        database != null && discordOAuth2Client != null -> DiscordUserAuthenticationRepository(database, discordOAuth2Client)
        else -> null
    }

    val accountLinkRepository = database?.let { AccountLinkRepository(database) }

    val linkedRolesUpdateService = when {
        accountLinkRepository != null && discordUserAuthenticationRepository != null -> LinkedRolesUpdateService(
            accountLinkRepository = accountLinkRepository,
            discordUserAuthenticationRepository = discordUserAuthenticationRepository,
            finishedGameRepository = repositories.finishedGames,
            profileRepository = repositories.profiles,
        )
        else -> null
    }

    val _ = HeXODiscordBot(
        repositories = repositories,
        accountLinkRepository = accountLinkRepository,
        discordUserAuthenticationRepository = discordUserAuthenticationRepository,
        linkedRolesUrl = if (linkedRolesUpdateService != null && config.server != null) "${config.server.url}/linked-roles" else null,
        token = config.bot.token,
    )

    if (discordUserAuthenticationRepository != null && config.server != null) {
        val server = HttpServer(
            discordUserAuthenticationRepository = discordUserAuthenticationRepository,
            linkedRolesUpdateService = linkedRolesUpdateService,
            port = config.server.port,
        )
        server.start()
    }

    println("""
     _    _     __   ______      ____        _          __ 
    | |  | |    \ \ / / __ \    |  _ \      | |        /_ |
    | |__| | ___ \ V / |  | |___| |_) | ___ | |_  __   _| |
    |  __  |/ _ \ > <| |  | |___|  _ < / _ \| __| \ \ / / |
    | |  | |  __// . \ |__| |   | |_) | (_) | |_   \ V /| |
    |_|  |_|\___/_/ \_\____/    |____/ \___/ \__|   \_/ |_|
    
    """.trimIndent())
}

val Manager.main get() = manager.bot as HeXODiscordBot

class HeXODiscordBot(
    private val repositories: HexoRepositories,
    private val accountLinkRepository: AccountLinkRepository?,
    private val discordUserAuthenticationRepository: DiscordUserAuthenticationRepository?,
    val linkedRolesUrl: String?,
    token: String,
) {
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

    private lateinit var gameMenu: MessageMenu<GameMenuParameter, *>
    private lateinit var profileMenu: MessageMenu<ProfileMenuParameter, *>
    private lateinit var leaderboardMenu: MessageMenu<Interaction, *>
    private lateinit var accountLinkMenu: MessageMenu<IModalCallback, *>

    val dtk = discordToolKit(jda, this)
        .withLocalization<_, DefaultLocalizationManager>()
        .withUIManager {
            localize()
            installErrorHandling()

            gameMenu = gameMenu(repositories.finishedGames)
            profileMenu = profileMenu(repositories.profiles)
            leaderboardMenu = leaderboardMenu(repositories.leaderboard, profileMenu)
            if (discordUserAuthenticationRepository != null && accountLinkRepository != null) {
                accountLinkMenu = accountLinkMenu(
                    discordAuthRepository = discordUserAuthenticationRepository,
                    accountLinkRepository = accountLinkRepository,
                    profileRepository = repositories.profiles,
                )
            }
        }
        .withCommandManager {
            localize()
            installErrorHandling()

            +renderHexoContextCommand()
            +renderHexoSlashCommand()

            +gameCommand(gameMenu)
            +leaderboardCommand(leaderboardMenu)
            +profileCommand(profileMenu)

            if (::accountLinkMenu.isInitialized) {
                +accountLinkCommand(accountLinkMenu)
            }

            updateCommands().queue()
        }
        .build()

    init {
        if (linkedRolesUrl != null) {
            logger.info { "Updating linked roles metadata..." }
            runBlocking {
                dtk.updateLinkedRoleMetadata()
            }
        }

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
