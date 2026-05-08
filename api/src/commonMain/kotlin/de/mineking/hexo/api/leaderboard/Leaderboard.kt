package de.mineking.hexo.api.leaderboard

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.InternalHexoApi
import de.mineking.hexo.api.profile.ProfileId
import de.mineking.hexo.api.utils.Instant

class Leaderboard(
    val generatedAt: Instant,
    val nextRefreshAt: Instant,
    val players: List<LeaderboardEntry>,
) {
    companion object {
        internal fun of(client: HexoApiClient, dto: LeaderboardDto): Leaderboard {
            return Leaderboard(
                generatedAt = dto.generatedAt,
                nextRefreshAt = dto.nextRefreshAt,
                players = dto.players.map {
                    LeaderboardEntry(
                        client = client,
                        profileId = it.profileId,
                        displayName = it.displayName,
                        image = it.image,
                        elo = it.elo,
                        gamesPlayed = it.gamesPlayed,
                        gamesWon = it.gamesWon,
                    )
                },
            )
        }
    }
}

class LeaderboardEntry(
    @property:InternalHexoApi val client: HexoApiClient,
    val profileId: ProfileId,
    val displayName: String,
    val image: String?,
    val elo: Int,
    val gamesPlayed: Int,
    val gamesWon: Int,
) {
    @OptIn(InternalHexoApi::class)
    suspend fun retrieveProfile() = client.profileRepository.getProfile(profileId)
}
