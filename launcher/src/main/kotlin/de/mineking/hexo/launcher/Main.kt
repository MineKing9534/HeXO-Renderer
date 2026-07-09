package de.mineking.hexo.launcher

import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.parse.cached
import de.mineking.hexo.board.parse.focusWinningRows
import de.mineking.hexo.board.render.cached
import de.mineking.hexo.board.render.image.BufferedImageBoardRenderer
import de.mineking.hexo.board.render.image.outputPngBytes
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.outputBoardAttachment
import de.mineking.hexo.bot.utils.LinkedRolesUpdateService
import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.caching.CachingRepositoryWrapper
import de.mineking.hexo.link.AccountLinkRepository
import de.mineking.hexo.link.HexoDatabaseManager
import de.mineking.hexo.link.oauth2.AESTokenTransform
import de.mineking.hexo.link.oauth2.DiscordOAuth2Client
import de.mineking.hexo.link.oauth2.DiscordUserAuthenticationRepository
import de.mineking.hexo.server.HttpServer
import de.mineking.hexo.server.services.LinkedRolesRedirectWebService
import de.mineking.hexo.server.services.OAUth2CallbackWebService
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

fun main() {
    val config = Config.fromEnvironment()
    val client = HdsApiClient(socketClient = null, repositoryWrapper = CachingRepositoryWrapper())

    val accountLinking = config.createAccountLinking()
    val linkedRoles = createLinkedRolesFeature(config, client, accountLinking)

    val pngRenderer = BufferedImageBoardRenderer.Default.outputPngBytes().cached()

    val bot = HeXODiscordBot(
        client = client,
        accountLinkRepository = accountLinking.accountLinkRepository,
        discordUserAuthenticationRepository = accountLinking.discordUserAuthenticationRepository,
        linkedRolesUrl = linkedRoles?.url,
        notationParser = BoardParser.createWithHdsSupport(client).focusWinningRows().cached(),
        boardRenderer = pngRenderer.outputBoardAttachment("png"),
        token = config.bot.token,
    )

    val httpServer = config.startHttpServer(accountLinking, linkedRoles)
    installShutdownHook(bot, httpServer)
    printBanner()
}

private data class AccountLinking(
    val discordOAuth2Client: DiscordOAuth2Client?,
    val discordUserAuthenticationRepository: DiscordUserAuthenticationRepository?,
    val accountLinkRepository: AccountLinkRepository?,
)

private data class LinkedRolesFeature(
    val updateService: LinkedRolesUpdateService,
    val url: String,
)

private fun Config.createAccountLinking(): AccountLinking {
    val database = database?.let { HexoDatabaseManager(it.url) }
    val discordOAuth2Client = createDiscordOAuth2Client()
    val discordUserAuthenticationRepository = createDiscordUserAuthenticationRepository(database, discordOAuth2Client)

    return AccountLinking(
        discordOAuth2Client = discordOAuth2Client,
        discordUserAuthenticationRepository = discordUserAuthenticationRepository,
        accountLinkRepository = database?.let(::AccountLinkRepository),
    )
}

private fun Config.createDiscordOAuth2Client(): DiscordOAuth2Client? {
    val oauth2 = oauth2 ?: return null
    val server = server ?: return null

    return DiscordOAuth2Client(
        clientId = oauth2.clientId,
        clientSecret = oauth2.clientSecret,
        redirectUri = server.oauth2CallbackUrl,
    )
}

private fun Config.createDiscordUserAuthenticationRepository(
    database: HexoDatabaseManager?,
    discordOAuth2Client: DiscordOAuth2Client?,
): DiscordUserAuthenticationRepository? {
    val oauth2 = oauth2 ?: return null
    if (database == null || discordOAuth2Client == null) return null

    return DiscordUserAuthenticationRepository(
        database = database,
        discordOAuth2Client = discordOAuth2Client,
        transform = AESTokenTransform(SecretKeySpec(Base64.decode(oauth2.encryptionKey), "AES")),
    )
}

private fun createLinkedRolesFeature(
    config: Config,
    client: HdsApiClient,
    accountLinking: AccountLinking,
): LinkedRolesFeature? {
    val server = config.server ?: return null
    val accountLinkRepository = accountLinking.accountLinkRepository ?: return null
    val discordUserAuthenticationRepository = accountLinking.discordUserAuthenticationRepository ?: return null

    return LinkedRolesFeature(
        updateService = LinkedRolesUpdateService(
            accountLinkRepository = accountLinkRepository,
            discordUserAuthenticationRepository = discordUserAuthenticationRepository,
            finishedGameRepository = client.finishedGameRepository,
            profileRepository = client.profileRepository,
        ),
        url = server.linkedRolesUrl,
    )
}

private fun Config.startHttpServer(accountLinking: AccountLinking, linkedRoles: LinkedRolesFeature?): HttpServer? {
    val server = server ?: return null
    val discordOAuth2Client = accountLinking.discordOAuth2Client ?: return null
    val discordUserAuthenticationRepository = accountLinking.discordUserAuthenticationRepository ?: return null

    val oAuth2WebService = OAUth2CallbackWebService(discordUserAuthenticationRepository) {
        linkedRoles?.updateService?.scheduleLinkedRoleDataUpdate(it)
    }

    val linkedRolesWebService = LinkedRolesRedirectWebService(oAuth2WebService, discordOAuth2Client)

    return HttpServer(
        services = listOf(oAuth2WebService, linkedRolesWebService),
        port = server.port,
    ).also { it.start(wait = false) }
}

private fun installShutdownHook(bot: HeXODiscordBot, httpServer: HttpServer?) {
    Runtime.getRuntime().addShutdownHook(
        Thread {
            httpServer?.stop()
            bot.shutdown()
        },
    )
}

private val ServerConfig.oauth2CallbackUrl get() = "$url/oauth2/callback"
private val ServerConfig.linkedRolesUrl get() = "$url/linked-roles"

private fun printBanner() {
    println("""
     _    _     __   ______      ____        _          __ 
    | |  | |    \ \ / / __ \    |  _ \      | |        /_ |
    | |__| | ___ \ V / |  | |___| |_) | ___ | |_  __   _| |
    |  __  |/ _ \ > <| |  | |___|  _ < / _ \| __| \ \ / / |
    | |  | |  __// . \ |__| |   | |_) | (_) | |_   \ V /| |
    |_|  |_|\___/_/ \_\____/    |____/ \___/ \__|   \_/ |_|
    
    """.trimIndent())
}
