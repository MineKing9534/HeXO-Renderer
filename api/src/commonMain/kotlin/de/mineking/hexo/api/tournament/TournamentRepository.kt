package de.mineking.hexo.api.tournament

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.game.FinishedGameRepository
import de.mineking.hexo.api.profile.ProfileRepository
import de.mineking.hexo.api.session.SessionRepository
import de.mineking.hexo.api.socket.TournamentUpdate
import de.mineking.hexo.api.utils.EntityState
import de.mineking.hexo.api.utils.withLock
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

interface TournamentRepository {
    suspend fun getTournament(id: TournamentId): Tournament?
    fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>>
}

internal class TournamentRepositoryImpl(
    private val client: HexoApiClient,
    private val profileRepository: ProfileRepository,
    private val finishedGameRepository: FinishedGameRepository,
    private val sessionRepository: SessionRepository,
) : TournamentRepository {
    init {
        if (client.socketClient != null) {
            client.coroutineScope.launch {
                client.socketClient.events
                    .filterIsInstance<TournamentUpdate>()
                    .collect { event ->
                        val _ = getTournament(event.tournamentId)
                    }
            }
        }
    }

    private val cacheLock = SynchronizedObject()
    private val cache = mutableMapOf<TournamentId, MutableStateFlow<EntityState<Tournament>>>()

    private val requester = client.entityRequesterFactory.createEntityRequester<TournamentId, Tournament> { id ->
        val response = client.request("/tournaments/${id.value}")
        val tournament = when {
            response.status.isSuccess() -> Tournament.of(client, profileRepository, finishedGameRepository, sessionRepository, response.body())
            else -> null
        }

        cacheLock.withLock {
            val state = tournament?.let { EntityState.Data(it) } ?: EntityState.NotFound
            cache[id]?.value = state

            if (tournament == null || tournament.status.isTerminal()) {
                cache -= id
            }
        }

        tournament
    }

    override suspend fun getTournament(id: TournamentId) = requester.fetch(id)

    override fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>> {
        if (client.socketClient == null) error("Cannot observe tournaments without a SocketIO connection")

        var shouldStartFetch = false
        val flow = cacheLock.withLock {
            cache.getOrPut(id) {
                shouldStartFetch = true
                MutableStateFlow(EntityState.Loading)
            }
        }

        if (shouldStartFetch) {
            client.coroutineScope.launch {
                val tournament = getTournament(id)
                cacheLock.withLock {
                    flow.value = when {
                        tournament == null -> EntityState.NotFound
                        else -> EntityState.Data(tournament)
                    }

                    if (tournament == null || tournament.status.isTerminal()) {
                        cache -= id
                    }
                }
            }
        }

        return flow
    }
}
