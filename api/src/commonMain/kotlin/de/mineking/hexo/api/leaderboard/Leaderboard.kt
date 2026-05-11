package de.mineking.hexo.api.leaderboard

import de.mineking.hexo.api.InternalHexoApi
import de.mineking.hexo.api.profile.ProfileId
import de.mineking.hexo.api.profile.ProfileRepository
import de.mineking.hexo.api.profile.ProfileStatistics
import de.mineking.hexo.api.utils.Instant

class Leaderboard(
    val generatedAt: Instant,
    val nextRefreshAt: Instant,
    val players: List<LeaderboardEntry>,
) {
    companion object {
        internal fun of(profileRepository: ProfileRepository, dto: LeaderboardDto): Leaderboard {
            return Leaderboard(
                generatedAt = dto.generatedAt,
                nextRefreshAt = dto.nextRefreshAt,
                players = dto.players.map {
                    LeaderboardEntry(
                        repository = profileRepository,
                        profileId = it.profileId,
                        displayName = it.displayName,
                        image = it.image,
                        elo = it.elo,
                        totalGames = ProfileStatistics.TotalGames(
                            played = it.gamesPlayed,
                            won = it.gamesWon,
                        ),
                    )
                },
            )
        }
    }
}

class LeaderboardEntry(
    private val repository: ProfileRepository,
    val profileId: ProfileId,
    val displayName: String,
    val image: String?,
    val elo: Int,
    val totalGames: ProfileStatistics.TotalGames,
) {
    @OptIn(InternalHexoApi::class)
    suspend fun retrieveProfile() = repository.getProfile(profileId)
}
