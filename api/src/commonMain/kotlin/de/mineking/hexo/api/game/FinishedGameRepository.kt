package de.mineking.hexo.api.game

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.profile.ProfileRepository
import de.mineking.hexo.api.tournament.TournamentRepository
import io.ktor.client.call.body
import io.ktor.http.isSuccess

// TODO support ongoing games

interface FinishedGameRepository {
    suspend fun getGame(id: GameId): FinishedGame?
}

internal class FinishedGameRepositoryImpl(
    private val client: HexoApiClient,
    private val profileRepository: ProfileRepository,
    private val tournamentRepository: () -> TournamentRepository,
) : FinishedGameRepository {
    private val requester = client.entityRequesterFactory.createEntityRequester<GameId, FinishedGame> {
        val response = client.request("/finished-games/${it.value}")

        if (!response.status.isSuccess()) return@createEntityRequester null
        FinishedGame.of(client, profileRepository, tournamentRepository, response.body())
    }

    override suspend fun getGame(id: GameId) = requester.fetch(id)
}
