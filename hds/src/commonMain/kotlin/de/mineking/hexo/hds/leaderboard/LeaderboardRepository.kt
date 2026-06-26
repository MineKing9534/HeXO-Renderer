package de.mineking.hexo.hds.leaderboard

import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.profile.ProfileRepository
import io.ktor.client.call.body
import io.ktor.http.isSuccess

interface LeaderboardRepository {
    suspend fun getLeaderboard(): Leaderboard
}

internal class LeaderboardRepositoryImpl(
    private val client: HdsApiClient,
    private val profileRepository: ProfileRepository,
) : LeaderboardRepository {
    private val requester = client.entityRequesterFactory.createEntityRequester<Unit, Leaderboard> {
        val response = client.request("/leaderboard")

        if (!response.status.isSuccess()) return@createEntityRequester null
        Leaderboard.of(profileRepository, response.body())
    }

    override suspend fun getLeaderboard() = requester.fetch(Unit)
        ?: error("Failed to fetch leaderboard")
}
