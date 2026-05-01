package de.mineking.hexo.discord

import de.mineking.discord.Manager
import de.mineking.discord.discordToolKit
import de.mineking.discord.localization.DefaultLocalizationManager
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.read
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.utils.await
import de.mineking.discord.utils.listen
import de.mineking.discord.withLocalization
import de.mineking.hexo.discord.commands.gameCommand
import de.mineking.hexo.discord.commands.renderHexoContextCommand
import de.mineking.hexo.discord.commands.renderHexoSlashCommand
import de.mineking.hexo.discord.menus.GameMenuParameter
import de.mineking.hexo.discord.menus.gameMenu
import de.mineking.hexo.history.MatchRepository
import de.mineking.hexo.history.cached
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
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.utils.messages.MessageRequest

internal val logger = KotlinLogging.logger {}

fun main() {
    MessageRequest.setDefaultUseComponentsV2(true)

    val token = System.getenv("TOKEN")
        ?: error("Missing required 'TOKEN' environment variable!")
    HeXODiscordBot(token)
}

val Manager.main get() = manager.bot as HeXODiscordBot

class HeXODiscordBot(token: String) {
    private val matchRepository = MatchRepository().cached()

    val jda = JDABuilder.createLight(token)
        .setStatus(OnlineStatus.ONLINE)
        .setActivity(Activity.playing("HeXO"))
        .build()

    val emojiManager = EmojiManager(jda)

    val notationParser = RectilinearStateBKETurnNotationParser.cached()
    val boardRenderer = ImageBoardRenderer.Default.cached()

    lateinit var gameMenu: MessageMenu<GameMenuParameter, *> private set

    val dtk = discordToolKit(jda, this)
        .withLocalization<_, DefaultLocalizationManager>()
        .withUIManager {
            localize()
            installErrorHandling()

            gameMenu = gameMenu(matchRepository)
        }
        .withCommandManager {
            localize()
            installErrorHandling()

            +renderHexoContextCommand()
            +renderHexoSlashCommand()

            +gameCommand(gameMenu)

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
