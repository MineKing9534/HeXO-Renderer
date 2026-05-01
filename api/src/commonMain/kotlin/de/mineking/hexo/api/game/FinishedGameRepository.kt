package de.mineking.hexo.api.game

import de.mineking.hexo.api.HexoApiClient
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlin.uuid.Uuid

interface FinishedGameRepository {
    suspend fun getGame(id: Uuid): FinishedGame?
}

internal class FinishedGameRepositoryImpl(private val client: HexoApiClient) : FinishedGameRepository {
    override suspend fun getGame(id: Uuid): FinishedGame? {
        val response = client.request("/finished-games/$id")

        if (!response.status.isSuccess()) return null
        return response.body()
    }
}
