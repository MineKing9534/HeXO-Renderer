package de.mineking.hexo.api.tournament

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.socket.HexoSocketEvent
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

internal class TournamentRepositoryImpl(private val client: HexoApiClient) : TournamentRepository {
    init {
        client.coroutineScope.launch {
            client.events
                .filterIsInstance<HexoSocketEvent.TournamentUpdate>()
                .collect { event ->
                    val _ = getTournament(event.tournamentId)
                }
        }
    }

    private val cacheLock = SynchronizedObject()
    private val cache = mutableMapOf<TournamentId, MutableStateFlow<EntityState<Tournament>>>()

    private val requester = client.entityRequesterFactory.createEntityRequester<TournamentId, Tournament> {
        val response = client.request("/tournaments/${it.value}")
        val tournament = when {
            response.status.isSuccess() -> Tournament.of(client, response.body<TournamentDto>())
            else -> null
        }

        cacheLock.withLock {
            if (tournament == null) {
                cache[it]?.value = EntityState.NotFound
                cache -= it
            } else {
                cache[it]?.value = EntityState.Data(tournament)
            }
        }

        tournament
    }

    override suspend fun getTournament(id: TournamentId) = requester.fetch(id)

    override fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>> {
        val flow = cacheLock.withLock {
            cache.getOrPut(id) {
                MutableStateFlow(EntityState.Loading)
            }
        }

        if (flow.value == EntityState.Loading) {
            client.coroutineScope.launch {
                val _ = getTournament(id)
            }
        }

        return flow
    }
}
