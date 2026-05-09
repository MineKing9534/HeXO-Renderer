package de.mineking.hexo.api.caching

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.RepositoryWrapper
import de.mineking.hexo.api.createRepositories
import de.mineking.hexo.api.game.FinishedGameRepository
import de.mineking.hexo.api.leaderboard.LeaderboardRepository
import de.mineking.hexo.api.profile.ProfileRepository
import de.mineking.hexo.api.tournament.TournamentRepository

private const val DEFAULT_CACHE_SIZE = 16L

class CachingRepositoryWrapper : RepositoryWrapper {
    override fun ProfileRepository.wrap(): ProfileRepository = CachingProfileRepository(this, DEFAULT_CACHE_SIZE)
    override fun LeaderboardRepository.wrap(): LeaderboardRepository = CachingLeaderboardRepository(this)
    override fun FinishedGameRepository.wrap(): FinishedGameRepository = CachingFinishedGameRepository(this, DEFAULT_CACHE_SIZE)
    override fun TournamentRepository.wrap(): TournamentRepository = CachingTournamentRepository(this, DEFAULT_CACHE_SIZE)
}

fun HexoApiClient.createCachingRepositories() = createRepositories(CachingRepositoryWrapper())
