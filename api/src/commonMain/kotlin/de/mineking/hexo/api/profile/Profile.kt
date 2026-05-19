package de.mineking.hexo.api.profile

import de.mineking.hexo.api.HEXO_WEBSITE
import de.mineking.hexo.api.utils.Instant
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ProfileId(val value: String)

interface Profile {
    val id: ProfileId
    val displayName: String
    val image: String?
    val registeredAt: Instant
    val lastActiveAt: Instant

    suspend fun retrieveStatistics(forceUpdate: Boolean = false): ProfileStatistics?

    val url get() = "${HEXO_WEBSITE}/profile/${id.value}"
}

interface RichProfile : Profile {
    val statistics: ProfileStatistics
}

internal class ProfileImpl(
    private val repository: ProfileRepositoryImpl,
    dto: ProfileDto,
) : Profile {
    override val id = dto.id
    override val displayName = dto.username
    override val image = dto.image
    override val registeredAt = dto.registeredAt
    override val lastActiveAt = dto.lastActiveAt

    override suspend fun retrieveStatistics(forceUpdate: Boolean) = repository.getProfileStatistics(id)
}

internal class RichProfileImpl(
    private val repository: ProfileRepositoryImpl,
    dto: ProfileDto,
    override val statistics: ProfileStatistics,
) : RichProfile, Profile by ProfileImpl(repository, dto) {
    override suspend fun retrieveStatistics(forceUpdate: Boolean): ProfileStatistics? {
        if (!forceUpdate) return statistics
        return repository.getProfileStatistics(id)
    }
}
