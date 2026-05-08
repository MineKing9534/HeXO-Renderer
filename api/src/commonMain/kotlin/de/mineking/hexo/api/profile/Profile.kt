package de.mineking.hexo.api.profile

import de.mineking.hexo.api.utils.Instant
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ProfileId(val value: String)

class Profile(
    val id: ProfileId,
    val displayName: String,
    val image: String?,
    val registeredAt: Instant,
    val lastActiveAt: Instant,
    val statistics: ProfileStatistics,
) {
    companion object {
        internal fun of(dto: ProfileDto, statistics: ProfileStatistics): Profile {
            return Profile(
                id = dto.id,
                displayName = dto.username,
                image = dto.image,
                registeredAt = dto.registeredAt,
                lastActiveAt = dto.lastActiveAt,
                statistics = statistics,
            )
        }
    }
}
