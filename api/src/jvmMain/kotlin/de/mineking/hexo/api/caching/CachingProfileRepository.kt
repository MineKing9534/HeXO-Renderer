package de.mineking.hexo.api.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import com.sksamuel.aedile.core.expireAfterWrite
import de.mineking.hexo.api.profile.Profile
import de.mineking.hexo.api.profile.ProfileId
import de.mineking.hexo.api.profile.ProfileRepository
import kotlin.time.Duration.Companion.minutes

internal class CachingProfileRepository(val delegate: ProfileRepository, cacheSize: Long) : ProfileRepository {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(3.minutes)
        .maximumSize(cacheSize)
        .asCache<ProfileId, Profile>()

    override suspend fun getProfile(id: ProfileId) = cache.getOrNull(id) { delegate.getProfile(it) }
}
