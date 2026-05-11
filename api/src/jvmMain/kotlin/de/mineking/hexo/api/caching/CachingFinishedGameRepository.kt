package de.mineking.hexo.api.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.api.game.FinishedGame
import de.mineking.hexo.api.game.FinishedGameRepository
import de.mineking.hexo.api.game.GameId

internal class CachingFinishedGameRepository(val delegate: FinishedGameRepository, cacheSize: Long) : FinishedGameRepository {
    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<GameId, FinishedGame>()

    override suspend fun getGame(id: GameId) = cache.getOrNull(id) { delegate.getGame(it) }
}
