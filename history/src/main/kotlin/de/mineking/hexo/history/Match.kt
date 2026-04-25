package de.mineking.hexo.history

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Player
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@JvmInline
@Serializable
value class PlayerId(val value: String)

@Serializable
data class Move(
    val playerId: PlayerId,
    @SerialName("x") val q: Int,
    @SerialName("y") val r: Int,
)

@Serializable
data class PlayerInfo(
    val playerId: PlayerId,
    val displayName: String,
    val elo: Int,
    val profileId: String,
)

fun PlayerInfo.isGuest() = playerId.value == profileId

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
    @SerialName("durationMs") val duration: @Serializable(with = DurationAsMillisecondsSerializer::class) Duration,
    val reason: GameFinishReason,
)

@Serializable
data class PlayerTile(val color: @Serializable(with = ColorSerializer::class) Color)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("mode")
sealed interface TimeControl {
    @Serializable
    @SerialName("unlimited")
    object Unlimited : TimeControl

    @Serializable
    @SerialName("turn")
    data class Turn(
        @SerialName("turnTimeMs") val turnTime: @Serializable(with = DurationAsMillisecondsSerializer::class) Duration,
    ) : TimeControl

    @Serializable
    @SerialName("match")
    data class Match(
        @SerialName("mainTimeMs") val mainTime: @Serializable(with = DurationAsMillisecondsSerializer::class) Duration,
        @SerialName("incrementMs") val increment: @Serializable(with = DurationAsMillisecondsSerializer::class) Duration,
    ) : TimeControl
}

@Serializable
data class GameOptions(
    val rated: Boolean,
    val timeControl: TimeControl,
)

@Serializable
data class Match(
    val id: Uuid,
    val players: List<PlayerInfo>,
    val playerTiles: Map<PlayerId, PlayerTile>,
    val moveCount: Int,
    val gameResult: GameResult,
    val gameOptions: GameOptions,
    val moves: List<Move>,
) {
    val playerIdMappings = mapOf(
        playerTiles.entries.first { (_, tile) -> tile.color.red > 200 }.key to Player.X,
        playerTiles.entries.first { (_, tile) -> tile.color.blue > 200 }.key to Player.O,
    )
    val playerMappings = playerIdMappings.entries.associate { (id, player) -> player to players.first { it.playerId == id } }

    fun asBoard(lastMove: Int = moveCount) = Board().apply {
        repeat(lastMove.coerceIn(0, moveCount)) {
            val move = moves[it]

            val cell = this[move.q, move.r]
            cell.owner = playerIdMappings[move.playerId]
            cell.turn = (it + 1) / 2

            if (it == lastMove - 1) cell.focussed = true
        }
    }
}

private object DurationAsMillisecondsSerializer : KSerializer<Duration> {
    override val descriptor = PrimitiveSerialDescriptor("Duration", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) = encoder.encodeLong(value.inWholeMilliseconds)
    override fun deserialize(decoder: Decoder) = (decoder.decodeLong() / 1000.0).roundToInt().seconds
}

private object ColorSerializer : KSerializer<Color> {
    override val descriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) = encoder.encodeString("#${"%06x".format(value.rgb and 0x00ffffff)}")
    override fun deserialize(decoder: Decoder): Color = Color.decode(decoder.decodeString())
}
