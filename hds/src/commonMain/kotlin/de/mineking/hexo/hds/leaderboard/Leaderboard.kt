package de.mineking.hexo.hds.leaderboard

import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.profile.ProfileId
import de.mineking.hexo.hds.profile.ProfileRepository
import de.mineking.hexo.hds.profile.ProfileStatistics
import de.mineking.hexo.hds.utils.Instant

class Leaderboard(
    val generatedAt: Instant,
    val nextRefreshAt: Instant,
    val players: List<LeaderboardEntry>,
) {
    companion object {
        internal fun of(client: HdsApiClient, dto: LeaderboardDto): Leaderboard {
            return Leaderboard(
                generatedAt = dto.generatedAt,
                nextRefreshAt = dto.nextRefreshAt,
                players = dto.players.map {
                    LeaderboardEntry(
                        repository = client.profileRepository,
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
    suspend fun retrieveProfile() = repository.getProfile(profileId)
}
