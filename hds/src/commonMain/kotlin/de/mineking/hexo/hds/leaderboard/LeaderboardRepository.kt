package de.mineking.hexo.hds.leaderboard

import de.mineking.hexo.hds.HdsApiClient
import io.ktor.client.call.body
import io.ktor.http.isSuccess

interface LeaderboardRepository {
    suspend fun getLeaderboard(): Leaderboard
}

internal class LeaderboardRepositoryImpl(private val client: HdsApiClient) : LeaderboardRepository {
    private val requester = client.entityRequesterFactory.createEntityRequester<Unit, Leaderboard> {
        val response = client.request("/leaderboard")

        if (!response.status.isSuccess()) return@createEntityRequester null
        Leaderboard.of(client, response.body())
    }

    override suspend fun getLeaderboard() = requester.fetch(Unit)
        ?: error("Failed to fetch leaderboard")
}
