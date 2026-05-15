package de.mineking.hexo.api

import de.mineking.hexo.api.formation.FormationRepository
import de.mineking.hexo.api.formation.FormationRepositoryImpl
import de.mineking.hexo.api.game.FinishedGameRepository
import de.mineking.hexo.api.game.FinishedGameRepositoryImpl
import de.mineking.hexo.api.leaderboard.LeaderboardRepository
import de.mineking.hexo.api.leaderboard.LeaderboardRepositoryImpl
import de.mineking.hexo.api.profile.ProfileRepository
import de.mineking.hexo.api.profile.ProfileRepositoryImpl
import de.mineking.hexo.api.tournament.TournamentRepository
import de.mineking.hexo.api.tournament.TournamentRepositoryImpl

data class HexoRepositories(
    val profiles: ProfileRepository,
    val leaderboard: LeaderboardRepository,
    val finishedGames: FinishedGameRepository,
    val tournaments: TournamentRepository,
    val formations: FormationRepository,
)

interface RepositoryWrapper {
    fun ProfileRepository.wrap(): ProfileRepository
    fun LeaderboardRepository.wrap(): LeaderboardRepository
    fun FinishedGameRepository.wrap(): FinishedGameRepository
    fun TournamentRepository.wrap(): TournamentRepository
    fun FormationRepository.wrap(): FormationRepository

    companion object {
        val Default = object : RepositoryWrapper {
            override fun ProfileRepository.wrap() = this
            override fun LeaderboardRepository.wrap() = this
            override fun FinishedGameRepository.wrap() = this
            override fun TournamentRepository.wrap() = this
            override fun FormationRepository.wrap() = this
        }
    }
}

fun HexoApiClient.createRepositories(wrapper: RepositoryWrapper = RepositoryWrapper.Default) = wrapper.run {
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
    tournamentRepository = TournamentRepositoryImpl(
        client = this@createRepositories,
        profileRepository = profileRepository,
        finishedGameRepository = finishedGameRepository,
    ).wrap()
    val formationRepository = FormationRepositoryImpl(client = this@createRepositories).wrap()

    HexoRepositories(
        profiles = profileRepository,
        leaderboard = leaderboardRepository,
        finishedGames = finishedGameRepository,
        tournaments = tournamentRepository,
        formations = formationRepository,
    )
}
