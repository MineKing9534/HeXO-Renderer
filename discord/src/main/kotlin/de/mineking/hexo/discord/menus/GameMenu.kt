package de.mineking.hexo.discord.menus

import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.MutableState
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.append
import de.mineking.discord.ui.builder.code
import de.mineking.discord.ui.builder.components.buildTextDisplay
import de.mineking.discord.ui.builder.components.localizedTextDisplay
import de.mineking.discord.ui.builder.components.message.actionRow
import de.mineking.discord.ui.builder.components.message.button
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.mediaGallery
import de.mineking.discord.ui.builder.components.message.modalButton
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.components.modal.intInput
import de.mineking.discord.ui.builder.components.modal.localizedLabel
import de.mineking.discord.ui.builder.components.modal.unbox
import de.mineking.discord.ui.builder.line
import de.mineking.discord.ui.disabledIf
import de.mineking.discord.ui.getValue
import de.mineking.discord.ui.initialize
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.message.MessageComponent
import de.mineking.discord.ui.message.MessageMenuConfig
import de.mineking.discord.ui.message.parameter
import de.mineking.discord.ui.message.withParameter
import de.mineking.discord.ui.modal.map
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedMenu
import de.mineking.discord.ui.renderValue
import de.mineking.discord.ui.setValue
import de.mineking.discord.ui.state
import de.mineking.discord.ui.terminateRender
import de.mineking.hexo.core.Player
import de.mineking.hexo.discord.EmojiType
import de.mineking.hexo.discord.MessageColor
import de.mineking.hexo.discord.effectiveLocale
import de.mineking.hexo.discord.main
import de.mineking.hexo.discord.render
import de.mineking.hexo.discord.respond
import de.mineking.hexo.history.MatchRepository
import dev.freya02.jda.emojis.unicode.Emojis
import net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import kotlin.uuid.Uuid

data class GameMenuParameter(val event: IReplyCallback, val id: Uuid, val move: Int)

fun UIManager.gameMenu(matchRepository: MatchRepository) = registerLocalizedMenu<GameMenuParameter, GameMenuLocalization>("game") { localization ->
    var id by state(Uuid.NIL)
    val moveState = state(0)
    var move by moveState

    initialize {
        id = it.id
        move = it.move
    }

    +container {
        val match = renderValue {
            val event = parameter({ error("") }, { it.event }, { event })
            localize(event.effectiveLocale) // Predefine locale for potential error handling

            val match = matchRepository.getGame(id)
            if (match == null) {
                event.respond(MessageColor.Error, localization.errorMatchNotFound(event.effectiveLocale, id))
                terminateRender()
            }

            localize(event.effectiveLocale) {
                bindParameter("id", id)
                bindParameter("match", match)
            }

            move = move.coerceIn(0, match.moveCount)
            val board = match.asBoard(move)

            +localizedTextDisplay("title")
            +separator(invisible = true, spacing = Separator.Spacing.LARGE)

            +buildTextDisplay {
                fun playerLine(emoji: EmojiType, player: Player) = line {
                    append(main.emojiManager[emoji].formatted)
                    append(" ")
                    append(match.playerMappings[player]!!.displayName)
                }

                +playerLine(EmojiType.PlayerX, Player.X)
                +playerLine(EmojiType.PlayerO, Player.O)

                +line()
                +line {
                    +code("${match.moveCount} ${localization.labelMoves(event.effectiveLocale)}")
                    append(" ")
                    +code(match.gameResult.duration.toString())
                }
            }

            +separator(spacing = Separator.Spacing.LARGE)
            +mediaGallery(MediaGalleryItem.fromFile(board.render(0)))
            +separator(spacing = Separator.Spacing.LARGE)

            match
        }

        +moveSelector("move", match?.moveCount ?: Int.MAX_VALUE, moveState)
    }
}


private fun MessageMenuConfig<*, *>.moveSelector(
    name: String,
    max: Int,
    ref: MutableState<Int>,
): MessageComponent<ActionRow> {
    var move by ref

    return actionRow {
        +button("$name-first", label = ZERO_WIDTH_SPACE, emoji = Emojis.REWIND) {
            move = 1
        }.disabledIf(move == 1)

        +button(
            "$name-back",
            label = ZERO_WIDTH_SPACE,
            emoji = Emojis.ARROW_LEFT,
        ) { move-- }.disabledIf(move <= 1)

        +modalButton(
            name,
            label = "$move",
            component = localizedLabel(
                intInput(
                    "move",
                    value = move,
                    placeholder = "$move"
                ).unbox().map { it ?: terminateRender() }
            ),
        ) { move = it.coerceIn(1, max) }

        +button(
            "$name-next",
            label = ZERO_WIDTH_SPACE,
            emoji = Emojis.ARROW_RIGHT,
        ) { move++ }.disabledIf(move >= max)

        +button("$name-last", label = ZERO_WIDTH_SPACE, emoji = Emojis.FAST_FORWARD) {
            move = parameter()
        }.disabledIf(move == max).withParameter(max)
    }
}


interface GameMenuLocalization : LocalizationFile {
    @Localize
    fun errorMatchNotFound(@Locale locale: DiscordLocale, @LocalizationParameter id: Uuid): String

    @Localize
    fun labelMoves(@Locale locale: DiscordLocale): String
}
