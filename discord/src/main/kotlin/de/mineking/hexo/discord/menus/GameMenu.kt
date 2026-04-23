package de.mineking.hexo.discord.menus

import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.Lazy
import de.mineking.discord.ui.MutableState
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.append
import de.mineking.discord.ui.builder.code
import de.mineking.discord.ui.builder.codeBlock
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
import de.mineking.discord.ui.lazy
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.message.MessageComponent
import de.mineking.discord.ui.message.MessageMenuConfig
import de.mineking.discord.ui.message.parameter
import de.mineking.discord.ui.message.withParameter
import de.mineking.discord.ui.modal.map
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedMenu
import de.mineking.discord.ui.render
import de.mineking.discord.ui.setValue
import de.mineking.discord.ui.state
import de.mineking.discord.ui.terminateRender
import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Player
import de.mineking.hexo.discord.EmojiType
import de.mineking.hexo.discord.MessageColor
import de.mineking.hexo.discord.effectiveLocale
import de.mineking.hexo.discord.main
import de.mineking.hexo.discord.render
import de.mineking.hexo.discord.respond
import de.mineking.hexo.history.GameFinishReason
import de.mineking.hexo.history.Match
import de.mineking.hexo.history.MatchRepository
import de.mineking.hexo.history.TimeControl
import de.mineking.hexo.history.isGuest
import de.mineking.hexo.render.RectilinearNotationType
import de.mineking.hexo.render.renderAsText
import dev.freya02.jda.emojis.unicode.Emojis
import net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import kotlin.uuid.Uuid

data class GameMenuParameter(val event: IReplyCallback, val id: Uuid, val move: Int)
private data class MatchData(val match: Match, val board: Board)

fun UIManager.gameMenu(matchRepository: MatchRepository) = registerLocalizedMenu<GameMenuParameter, GameMenuLocalization>("game") { localization ->
    var id by state(Uuid.NIL)
    val moveState = state(0)
    var move by moveState

    initialize {
        id = it.id
        move = it.move
    }

    val locale = parameter({ DiscordLocale.UNKNOWN }, { it.event.effectiveLocale }, { event.effectiveLocale })
    localize(locale) // Predefine locale for potential error handling

    val lazyMatchData = lazy(default = null) {
        val match = matchRepository.getGame(id) ?: return@lazy null
        val board = match.asBoard(move)

        MatchData(match, board)
    }
    val matchData by lazyMatchData

    render {
        val matchData = matchData
        val event = parameter({ error("") }, { it.event }, { event })
        if (matchData == null) {
            event.respond(MessageColor.Error, localization.errorMatchNotFound(event.effectiveLocale, id))
            terminateRender()
        }

        localize(locale) {
            bindParameter("match", matchData.match)
        }

        move = move.coerceIn(0, matchData.match.moveCount)
    }

    +container {
        render {
            val (match, board) = matchData ?: return@render

            +localizedTextDisplay("title")
            +separator(invisible = true)

            +buildTextDisplay {
                fun playerLine(emoji: EmojiType, player: Player) = line {
                    val playerInfo = match.playerMappings[player]!!

                    append(main.emojiManager[emoji].formatted)
                    append(" ")
                    append(playerInfo.displayName)

                    if (!playerInfo.isGuest()) append(" (`${playerInfo.elo} ELO`)")
                    if (match.gameResult.winningPlayerId == playerInfo.playerId) append(" :first_place:")
                }

                listOf(EmojiType.PlayerX to Player.X, EmojiType.PlayerO to Player.O)
                    .sortedBy { (_, player) -> match.moves.indexOfFirst { match.playerIdMappings[it.playerId] == player } }
                    .forEach { (emoji, player) -> +playerLine(emoji, player) }

                +line()
                +line {
                    +code("${Emojis.HOURGLASS.formatted} ${match.gameResult.duration}")
                    append("\u2003")
                    +code("${Emojis.TIMER_CLOCK.formatted} ${localization.timeControl(locale, match.gameOptions.timeControl)}")
                    append("\u2003")
                    +code(match.gameResult.reason.localize(locale, localization))
                }
            }

            +separator(spacing = Separator.Spacing.LARGE)
            +mediaGallery(MediaGalleryItem.fromFile(board.render(0)))
            +separator(spacing = Separator.Spacing.LARGE)

            match to board
        }

        +moveSelector("move", matchData?.match?.moveCount ?: Int.MAX_VALUE, moveState)
        +additionalActions(lazyMatchData)
    }
}

private fun additionalActions(matchData: Lazy<MatchData?>): MessageComponent<ActionRow> {
    val matchData by matchData
    return actionRow {
        +button("format", emoji = Emojis.PRINTER) {
            val board = matchData?.board ?: return@button
            deferReply(true).queue()

            hook.editOriginalComponents(container {
                RectilinearNotationType.entries.forEach {
                    +buildTextDisplay {
                        +codeBlock("hexo", board.renderAsText(it))
                    }
                }
            }.render()).useComponentsV2().queue()
        }
    }
}

private fun GameFinishReason.localize(locale: DiscordLocale, localization: GameMenuLocalization): String {
    val emoji = when (this) {
        GameFinishReason.SixInARow -> Emojis.TRIANGULAR_RULER
        GameFinishReason.Timeout -> Emojis.ALARM_CLOCK
        GameFinishReason.Surrender -> Emojis.FLAG_WHITE
        GameFinishReason.Disconnect -> Emojis.SATELLITE
        GameFinishReason.DrawAgreement -> Emojis.HANDSHAKE
        GameFinishReason.Terminated -> Emojis.NO_ENTRY
    }

    return "${emoji.formatted} ${localization.finishReason(locale, this)}"
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
            label = "$move / $max",
            emoji = Emojis.PUZZLE_PIECE,
            component = localizedLabel(
                intInput(
                    "move",
                    value = move,
                    placeholder = "$move",
                ).unbox().map { it ?: terminateRender() },
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
    fun timeControl(@Locale locale: DiscordLocale, @LocalizationParameter control: TimeControl): String

    @Localize
    fun finishReason(@Locale locale: DiscordLocale, @LocalizationParameter reason: GameFinishReason): String
}
