package de.mineking.hexo.history

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
import kotlin.jvm.JvmInline
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

typealias Duration = @Serializable(with = DurationAsMillisecondsSerializer::class) kotlin.time.Duration

@Serializable(with = ColorSerializer::class)
data class Color(val red: Int, val green: Int, val blue: Int) {
    init {
        require(red in 0..255 && green in 0..255 && blue in 0..255)
    }

    private val Int.hex get() = toString(16).uppercase().padStart(2, '0')
    fun format() = "#${red.hex}${green.hex}${blue.hex}"

    companion object {
        fun decode(input: String): Color {
            val s = input.removePrefix("#")
            require(s.length == 6)

            return Color(
                red = s.substring(0, 2).toInt(16),
                green = s.substring(2, 4).toInt(16),
                blue = s.substring(4, 6).toInt(16),
            )
        }
    }
}

@JvmInline
@Serializable
value class PlayerId(val value: String)

@Serializable
data class Move(
    @SerialName("playerId") val id: PlayerId,
    @SerialName("x") val q: Int,
    @SerialName("y") val r: Int,
)

@Serializable
data class PlayerInfo(
    @SerialName("playerId") val id: PlayerId,
    val displayName: String,
    val profileId: String,
    val elo: Int,
    val eloChange: Int?,
)

fun PlayerInfo.isGuest() = id.value == profileId

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
        @SerialName("turnTimeMs") val turnTime: Duration,
    ) : TimeControl

    @Serializable
    @SerialName("match")
    data class Match(
        @SerialName("mainTimeMs") val mainTime: Duration,
        @SerialName("incrementMs") val increment: Duration,
    ) : TimeControl
}

@Serializable
data class GameOptions(
    val rated: Boolean,
    val timeControl: TimeControl,
)

@Serializable
data class Tournament(
    @SerialName("tournamentId") val id: Uuid,
    @SerialName("tournamentName") val name: String,
    val round: Int,
    val order: Int,
    val bestOf: Int,
    val currentGameNumber: Int,
) {
    val url get() = "$HEXO_WEBSITE/tournaments/$id"
}

@Serializable
data class Match(
    val id: Uuid,
    val players: List<PlayerInfo>,
    val playerTiles: Map<PlayerId, PlayerTile>,
    val moveCount: Int,
    val gameResult: GameResult,
    val gameOptions: GameOptions,
    val tournament: Tournament?,
    val moves: List<Move>,
) {
    val url get() = "$HEXO_WEBSITE/games/$id"

    val playerIdMappings = mapOf(
        playerTiles.entries.first { (_, tile) -> tile.color.red > 200 }.key to Player.X,
        playerTiles.entries.first { (_, tile) -> tile.color.blue > 200 }.key to Player.O,
    )
    val playerMappings = playerIdMappings.entries.associate { (id, player) -> player to players.first { it.id == id } }
}

private object DurationAsMillisecondsSerializer : KSerializer<Duration> {
    override val descriptor = PrimitiveSerialDescriptor("Duration", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) = encoder.encodeLong(value.inWholeMilliseconds)
    override fun deserialize(decoder: Decoder) = (decoder.decodeLong() / 1000.0).roundToInt().seconds
}

internal object ColorSerializer : KSerializer<Color> {
    override val descriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) = encoder.encodeString(value.format())
    override fun deserialize(decoder: Decoder): Color = Color.decode(decoder.decodeString())
}
