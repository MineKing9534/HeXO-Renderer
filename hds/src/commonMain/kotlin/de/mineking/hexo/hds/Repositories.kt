package de.mineking.hexo.hds

import de.mineking.hexo.hds.formation.FormationRepository
import de.mineking.hexo.hds.formation.FormationRepositoryImpl
import de.mineking.hexo.hds.game.FinishedGameRepository
import de.mineking.hexo.hds.game.FinishedGameRepositoryImpl
import de.mineking.hexo.hds.leaderboard.LeaderboardRepository
import de.mineking.hexo.hds.leaderboard.LeaderboardRepositoryImpl
import de.mineking.hexo.hds.profile.ProfileRepository
import de.mineking.hexo.hds.profile.ProfileRepositoryImpl
import de.mineking.hexo.hds.session.SessionRepository
import de.mineking.hexo.hds.session.SessionRepositoryImpl
import de.mineking.hexo.hds.tournament.TournamentRepository
import de.mineking.hexo.hds.tournament.TournamentRepositoryImpl

data class HexoRepositories(
    val profiles: ProfileRepository,
    val leaderboard: LeaderboardRepository,
    val finishedGames: FinishedGameRepository,
    val tournaments: TournamentRepository,
    val formations: FormationRepository,
    val sessions: SessionRepository,
)

interface RepositoryWrapper {
    fun ProfileRepository.wrap(): ProfileRepository
    fun LeaderboardRepository.wrap(): LeaderboardRepository
    fun FinishedGameRepository.wrap(): FinishedGameRepository
    fun TournamentRepository.wrap(): TournamentRepository
    fun FormationRepository.wrap(): FormationRepository
    fun SessionRepository.wrap(): SessionRepository

    companion object {
        val Default = object : RepositoryWrapper {
            override fun ProfileRepository.wrap() = this
            override fun LeaderboardRepository.wrap() = this
            override fun FinishedGameRepository.wrap() = this
            override fun TournamentRepository.wrap() = this
            override fun FormationRepository.wrap() = this
            override fun SessionRepository.wrap() = this
        }
    }
}

fun HdsApiClient.createRepositories(wrapper: RepositoryWrapper = RepositoryWrapper.Default) = wrapper.run {
    lateinit var tournamentRepository: TournamentRepository
    val profileRepository = ProfileRepositoryImpl(client = this@createRepositories).wrap()
    val finishedGameRepository = FinishedGameRepositoryImpl(
        client = this@createRepositories,
        profileRepository = profileRepository,
        tournamentRepository = { tournamentRepository },
    ).wrap()
    val leaderboardRepository = LeaderboardRepositoryImpl(
        client = this@createRepositories,
        profileRepository = profileRepository,
    ).wrap()
    val sessionRepository = SessionRepositoryImpl(
        client = this@createRepositories,
        profileRepository = profileRepository,
        tournamentRepository = { tournamentRepository },
    ).wrap()
    tournamentRepository = TournamentRepositoryImpl(
        client = this@createRepositories,
        profileRepository = profileRepository,
        finishedGameRepository = finishedGameRepository,
        sessionRepository = sessionRepository,
    ).wrap()
    val formationRepository = FormationRepositoryImpl(client = this@createRepositories).wrap()

    HexoRepositories(
        profiles = profileRepository,
        leaderboard = leaderboardRepository,
        finishedGames = finishedGameRepository,
        tournaments = tournamentRepository,
        formations = formationRepository,
        sessions = sessionRepository,
    )
}
