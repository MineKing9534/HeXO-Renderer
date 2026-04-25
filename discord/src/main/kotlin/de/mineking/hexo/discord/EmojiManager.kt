package de.mineking.hexo.discord

import de.mineking.discord.utils.await
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.emoji.Emoji

enum class CustomEmoji(val path: String) {
    PlayerX("hexo_player_x"),
    PlayerO("hexo_player_o"),

    SwitchOn("switch_on"),
    SwitchOff("switch_off"),
}

val CustomEmoji.discordName get() = name.lowercase()

class EmojiManager(val jda: JDA) {
    private val emojis = mutableMapOf<CustomEmoji, Emoji>()

    init {
        runBlocking {
            val data = jda.retrieveApplicationEmojis().await()
            data.filter { emoji -> CustomEmoji.entries.none { it.discordName == emoji.name } }.forEach {
                logger.info { "Deleting unused emoji: ${it.name}" }
                it.delete().queue()
            }

            emojis += CustomEmoji.entries.associateWith { type ->
                val emoji = data.find { type.discordName == it.name }

                val currentImage = emoji?.image?.downloadAsIcon()?.await()
                val expectedImage = loadIcon(type)

                if (currentImage?.encoding == expectedImage.encoding) {
                    emoji
                } else {
                    if (emoji == null) {
                        logger.info { "Creating missing emoji: ${type.discordName}" }
                    } else {
                        logger.info { "Overwriting existing emoji: ${type.discordName}" }
                    }

                    val createAction = jda.createApplicationEmoji(type.discordName, expectedImage)
                    val action = emoji?.delete()?.flatMap { createAction } ?: createAction
                    action.await()
                }
            }
        }
    }

    operator fun get(type: CustomEmoji) = emojis[type]!!
}

private fun loadIcon(type: CustomEmoji) =
    EmojiManager::class.java.classLoader.getResourceAsStream("emoji/${type.path}.png")?.use { Icon.from(it) }
        ?: error("Could not load icon for emoji type: $type")
