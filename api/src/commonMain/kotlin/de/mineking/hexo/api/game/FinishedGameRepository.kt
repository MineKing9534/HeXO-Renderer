package de.mineking.hexo.api.game

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.profile.ProfileRepository
import de.mineking.hexo.api.tournament.TournamentRepository
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

// TODO support ongoing games

interface FinishedGameRepository {
    suspend fun getGame(id: GameId): FinishedGame?

    suspend fun getFinishedGames(page: Int, pageSize: Int, rated: Boolean? = null): List<FinishedGame>
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

    private val listRequester = client.entityRequesterFactory.createEntityRequester<FinishedGamesParameter, List<FinishedGame>> { param ->
        val response = client.request("/finished-games") {
            parameter("page", param.page)
            parameter("pageSize", param.pageSize)
            parameter("rated", when (param.rated) {
                true -> "rated"
                false -> "unrated"
                else -> "all"
            })
        }

        if (!response.status.isSuccess()) return@createEntityRequester null

        @Serializable
        data class Response(val games: List<FinishedGameDto>)
        response.body<Response>().games
            .map { FinishedGame.of(client, profileRepository, tournamentRepository, it) }
    }

    override suspend fun getGame(id: GameId) = requester.fetch(id)
    override suspend fun getFinishedGames(page: Int, pageSize: Int, rated: Boolean?) =
        listRequester.fetch(FinishedGamesParameter(page, pageSize, rated)) ?: emptyList()

    data class FinishedGamesParameter(val page: Int, val pageSize: Int, val rated: Boolean?)
}
