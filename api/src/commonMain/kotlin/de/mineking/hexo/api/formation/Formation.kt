package de.mineking.hexo.api.formation

import de.mineking.hexo.core.CellOwner
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class FormationId(val value: String)

@Serializable
data class Formation(
    val id: FormationId,
    val name: String,
    val gamePosition: GamePosition,
)

@Serializable
data class GamePosition(
    val currentTurnPlayer: @Serializable(with = FormationCellOwnerSerializer::class) CellOwner,
    val placementsRemaining: Int,
    val cells: List<GamePositionCell>,
)

@Serializable
data class GamePositionCell(
    @SerialName("x") val q: Int,
    @SerialName("y") val r: Int,
    val player: @Serializable(with = FormationCellOwnerSerializer::class) CellOwner,
)

internal object FormationCellOwnerSerializer : KSerializer<CellOwner> {
    override val descriptor = PrimitiveSerialDescriptor("CellOwner", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = when (val value = decoder.decodeString()) {
        "player-1" -> CellOwner.X
        "player-2" -> CellOwner.O
        else -> throw IllegalArgumentException("Unknown cell owner '$value'")
    }

    override fun serialize(encoder: Encoder, value: CellOwner) = throw UnsupportedOperationException()
}
