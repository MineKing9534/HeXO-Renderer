package de.mineking.hexo.hds.caching

import de.mineking.hexo.hds.leaderboard.Leaderboard
import de.mineking.hexo.hds.leaderboard.LeaderboardRepository
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Clock

internal class CachingLeaderboardRepository(private val delegate: LeaderboardRepository) : LeaderboardRepository {
    private val lock = ReentrantReadWriteLock()
    private var cache: Leaderboard? = null

    override suspend fun getLeaderboard(): Leaderboard {
        lock.read {
            val cached = cache?.takeIf { it.nextRefreshAt > Clock.System.now() }
            if (cached != null) return cached
        }

        val fetched = delegate.getLeaderboard()
        lock.write { cache = fetched }
        return fetched
    }
}
