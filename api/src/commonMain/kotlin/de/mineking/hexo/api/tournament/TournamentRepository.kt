package de.mineking.hexo.api.tournament

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.utils.EntityRequester
import io.ktor.client.call.body
import io.ktor.http.isSuccess

// TODO optionally track tournament updates

interface TournamentRepository {
    suspend fun getTournament(id: TournamentId): Tournament?
}

internal class TournamentRepositoryImpl(private val client: HexoApiClient) : TournamentRepository {
    private val requester = EntityRequester<TournamentId, Tournament>(client) {
        val response = request("/tournaments/${it.value}")

        if (!response.status.isSuccess()) return@EntityRequester null
        Tournament.of(client, response.body<TournamentDto>())
    }

    override suspend fun getTournament(id: TournamentId) = requester.fetch(id)
}
