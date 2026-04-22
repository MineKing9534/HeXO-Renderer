package de.mineking.hexo.history

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.Player
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
)

@Serializable
data class GameResult(
    val winningPlayerId: PlayerId,
    @SerialName("durationMs") val duration: @Serializable(with = DurationAsMillisecondsSerializer::class) Duration,
)

@Serializable
data class PlayerTile(val color: @Serializable(with = ColorSerializer::class) Color)

@Serializable
data class Match(
    val players: List<PlayerInfo>,
    val playerTiles: Map<PlayerId, PlayerTile>,
    val moveCount: Int,
    val gameResult: GameResult,
    val moves: List<Move>,
) {
    val playerIdMappings = mapOf(
        playerTiles.entries.first { (_, tile) -> tile.color.red > 200 }.key to Player.X,
        playerTiles.entries.first { (_, tile) -> tile.color.blue > 200 }.key to Player.O,
    )
    val playerMappings = playerIdMappings.entries.associate { (id, player) -> player to players.first { it.playerId == id } }

    fun asBoard(lastMove: Int) = Board().apply {
        repeat(lastMove) {
            val move = moves[it]

            val cell = this[move.q, move.r]
            cell.owner = playerIdMappings[move.playerId]
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
