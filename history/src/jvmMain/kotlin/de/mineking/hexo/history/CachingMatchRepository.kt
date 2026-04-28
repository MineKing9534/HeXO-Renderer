package de.mineking.hexo.history

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import kotlin.uuid.Uuid

fun MatchRepository.cached(cacheSize: Long = 16): MatchRepository = when (this) {
    is CachingMatchRepository -> this
    else -> CachingMatchRepository(this, cacheSize)
}

private class CachingMatchRepository(val delegate: MatchRepository, cacheSize: Long) : MatchRepository {
    private val cache = Caffeine.newBuilder().maximumSize(cacheSize).asCache<Uuid, Match>()

    override suspend fun getGame(id: Uuid) = cache.getOrNull(id) { delegate.getGame(it) }
}

