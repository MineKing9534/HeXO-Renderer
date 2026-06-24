package de.mineking.hexo.api.formation

import de.mineking.hexo.api.AbstractGamePosition
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class FormationId(val value: String)

class Formation(
    val id: FormationId,
    val url: String,
    val name: String,
    val gamePosition: GamePosition,
) : AbstractGamePosition {
    override val moves get() = gamePosition.cells

    companion object {
        internal fun of(host: String, dto: FormationDto): Formation {
            return Formation(
                id = dto.id,
                url = "$host/sandbox/${dto.id.value}",
                name = dto.name,
                gamePosition = dto.gamePosition,
            )
        }
    }
}
