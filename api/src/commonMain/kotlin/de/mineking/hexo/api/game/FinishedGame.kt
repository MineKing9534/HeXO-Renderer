package de.mineking.hexo.api.game

import de.mineking.hexo.api.HEXO_WEBSITE
import de.mineking.hexo.api.ProfileId
import de.mineking.hexo.api.tournament.TournamentId
import de.mineking.hexo.api.utils.Color
import de.mineking.hexo.api.utils.Duration
import de.mineking.hexo.api.utils.TimeControl
import de.mineking.hexo.core.Player
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@JvmInline
@Serializable
value class PlayerId(val value: String)

@JvmInline
@Serializable
value class GameId(val value: Uuid)

@Serializable
data class FinishedGame(
    val id: GameId,
    val players: List<PlayerInfo>,
    val playerTiles: Map<PlayerId, PlayerTile>,
    val moveCount: Int,
    val gameResult: GameResult,
    val gameOptions: GameOptions,
    val tournament: TournamentSnapshot?,
    val moves: List<Move>,
) {
    val url get() = "${HEXO_WEBSITE}/games/${id.value}"

    val playerIdMappings = mapOf(
        playerTiles.entries.first { (_, tile) -> tile.color.red > 200 }.key to Player.X,
        playerTiles.entries.first { (_, tile) -> tile.color.blue > 200 }.key to Player.O,
    )
    val playerMappings = playerIdMappings.entries.associate { (id, player) -> player to players.first { it.id == id } }
}

@Serializable
data class TournamentSnapshot(
    @SerialName("tournamentId") val id: TournamentId,
    @SerialName("tournamentName") val name: String,
    val round: Int,
    val order: Int,
    val bestOf: Int,
    val currentGameNumber: Int,
) {
    val url get() = "${HEXO_WEBSITE}/tournaments/$id"
}

@Serializable
data class Move(
    @SerialName("playerId") val id: PlayerId,
    @SerialName("x") val q: Int,
    @SerialName("y") val r: Int,
)

@Serializable
data class PlayerTile(val color: Color)

@Serializable
data class PlayerInfo(
    @SerialName("playerId") val id: PlayerId,
    val profileId: ProfileId,
    val displayName: String,
    val elo: Int,
    val eloChange: Int?,
)

fun PlayerInfo.isGuest() = id.value == profileId.value

@Serializable
enum class GameFinishReason {
    @SerialName("six-in-a-row") SixInARow,
    @SerialName("timeout") Timeout,
    @SerialName("surrender") Surrender,
    @SerialName("disconnect") Disconnect,
    @SerialName("draw-agreement") DrawAgreement,
    @SerialName("terminated") Terminated,
}

@Serializable
data class GameResult(
    val winningPlayerId: PlayerId?,
    @SerialName("durationMs") val duration: Duration,
    val reason: GameFinishReason,
)

@Serializable
data class GameOptions(
    val rated: Boolean,
    val timeControl: TimeControl,
)
