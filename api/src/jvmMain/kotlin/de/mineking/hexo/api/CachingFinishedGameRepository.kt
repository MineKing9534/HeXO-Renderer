package de.mineking.hexo.api

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.api.game.FinishedGame
import de.mineking.hexo.api.game.FinishedGameRepository
import kotlin.uuid.Uuid

fun FinishedGameRepository.cached(cacheSize: Long = 16): FinishedGameRepository = when (this) {
    is CachingFinishedGameRepository -> this
    else -> CachingFinishedGameRepository(this, cacheSize)
}

private class CachingFinishedGameRepository(val delegate: FinishedGameRepository, cacheSize: Long) : FinishedGameRepository {
    private val cache = Caffeine.newBuilder().maximumSize(cacheSize).asCache<Uuid, FinishedGame>()

    override suspend fun getGame(id: Uuid) = cache.getOrNull(id) { delegate.getGame(it) }
}
