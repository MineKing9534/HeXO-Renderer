package de.mineking.hexo.api.game

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.utils.EntityRequester
import io.ktor.client.call.body
import io.ktor.http.isSuccess

// TODO support ongoing games

interface FinishedGameRepository {
    suspend fun getGame(id: GameId): FinishedGame?
}

internal class FinishedGameRepositoryImpl(private val client: HexoApiClient) : FinishedGameRepository {
    private val requester = EntityRequester<GameId, FinishedGame>(client) {
        val response = request("/finished-games/${it.value}")

        if (!response.status.isSuccess()) return@EntityRequester null
        FinishedGame.of(client, response.body())
    }

    override suspend fun getGame(id: GameId) = requester.fetch(id)
}
