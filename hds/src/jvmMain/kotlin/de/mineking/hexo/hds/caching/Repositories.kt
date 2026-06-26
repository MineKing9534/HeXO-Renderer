package de.mineking.hexo.hds.caching

import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.RepositoryWrapper
import de.mineking.hexo.hds.createRepositories
import de.mineking.hexo.hds.formation.FormationRepository
import de.mineking.hexo.hds.game.FinishedGameRepository
import de.mineking.hexo.hds.leaderboard.LeaderboardRepository
import de.mineking.hexo.hds.profile.ProfileRepository
import de.mineking.hexo.hds.session.SessionRepository
import de.mineking.hexo.hds.tournament.TournamentRepository

private const val DEFAULT_CACHE_SIZE = 16L

class CachingRepositoryWrapper : RepositoryWrapper {
    override fun ProfileRepository.wrap(): ProfileRepository = CachingProfileRepository(this, DEFAULT_CACHE_SIZE)
    override fun LeaderboardRepository.wrap(): LeaderboardRepository = CachingLeaderboardRepository(this)
    override fun FinishedGameRepository.wrap(): FinishedGameRepository = CachingFinishedGameRepository(this, DEFAULT_CACHE_SIZE)
    override fun TournamentRepository.wrap(): TournamentRepository = CachingTournamentRepository(this, DEFAULT_CACHE_SIZE)
    override fun FormationRepository.wrap(): FormationRepository = CachingFormationRepository(this, DEFAULT_CACHE_SIZE)
    override fun SessionRepository.wrap() = this
}

fun HdsApiClient.createCachingRepositories() = createRepositories(CachingRepositoryWrapper())
