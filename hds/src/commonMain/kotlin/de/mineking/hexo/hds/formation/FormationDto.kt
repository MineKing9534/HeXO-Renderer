package de.mineking.hexo.hds.formation

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.hds.game.Move
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class FormationDto(
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

@Serializable(with = GamePositionCellSerializer::class)
data class GamePositionCell(
    override val coordinate: CellCoordinate,
    override val owner: CellOwner,
) : Move

internal object GamePositionCellSerializer : KSerializer<GamePositionCell> {
    override val descriptor = GamePositionCell.serializer().descriptor
    override fun deserialize(decoder: Decoder) = GamePositionCellDto.serializer().deserialize(decoder).let {
        GamePositionCell(
            coordinate = CellCoordinate(it.q, it.r),
            owner = it.player,
        )
    }
    override fun serialize(encoder: Encoder, value: GamePositionCell) = throw UnsupportedOperationException()
}

@Serializable
internal data class GamePositionCellDto(
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
