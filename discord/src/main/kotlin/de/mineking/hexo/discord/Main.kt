package de.mineking.hexo.discord

import de.mineking.discord.discordToolKit
import de.mineking.discord.localization.DefaultLocalizationManager
import de.mineking.discord.localization.read
import de.mineking.discord.withLocalization
import de.mineking.hexo.discord.commands.renderHexoContextCommand
import de.mineking.hexo.discord.commands.renderHexoSlashCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.messages.MessageRequest

internal val logger = KotlinLogging.logger {}

fun main() {
    MessageRequest.setDefaultUseComponentsV2(true)

    val token = System.getenv("TOKEN")
        ?: error("Missing required 'TOKEN' environment variable!")
    HeXODiscordBot(token)
}

class HeXODiscordBot(token: String) {
    private val jda = JDABuilder.createLight(token)
        .setStatus(OnlineStatus.ONLINE)
        .setActivity(Activity.playing("HeXO"))
        .build()

    private val dtk = discordToolKit(jda)
        .withLocalization<_, DefaultLocalizationManager>()
        .withCommandManager {
            localize()
            installErrorHandling(manager.localizationManager.read())

            +renderHexoContextCommand()
            +renderHexoSlashCommand()

            updateCommands().queue()
        }
        .build()
}
