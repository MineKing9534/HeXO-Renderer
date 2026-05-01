package de.mineking.hexo.api.tournament

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.socket.HexoEvent
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface TournamentRepository {
    suspend fun getTournament(id: TournamentId): Tournament?
    suspend fun getLiveTournament(id: TournamentId): LiveTournament?
}

internal class TournamentRepositoryImpl(private val client: HexoApiClient) : TournamentRepository {
    init {
        client.coroutineScope.launch {
            client.events
                .filterIsInstance<HexoEvent.TournamentUpdate>()
                .collect { event ->
                    handleTournamentUpdateEvent(event.tournamentId)
                }
        }
    }

    private fun handleTournamentUpdateEvent(id: TournamentId) {
        client.coroutineScope.launch {
            val _ = getLiveTournament(id, useCache = false)
        }
    }

    private val cacheLock = Mutex()
    private val cache = mutableMapOf<TournamentId, LiveTournament>()

    private val waitingLock = Mutex()
    private val waiting = mutableMapOf<TournamentId, Deferred<Tournament?>>()

    override suspend fun getTournament(id: TournamentId): Tournament? {
        val deferred = waitingLock.withLock {
            waiting[id] ?: client.coroutineScope.async(start = CoroutineStart.LAZY) {
                val response = client.request("/tournaments/${id.value}")

                if (!response.status.isSuccess()) return@async null
                response.body<Tournament>()
            }.also {
                waiting[id] = it
                it.start()
            }
        }

        return try {
            deferred.await()
        } finally {
            waitingLock.withLock {
                if (waiting[id] === deferred) {
                    waiting -= id
                }
            }
        }
    }

    private suspend fun getLiveTournament(id: TournamentId, useCache: Boolean): LiveTournament? {
        if (useCache) {
            cacheLock.withLock {
                val cacheEntry = cache[id]
                if (cacheEntry != null) return cacheEntry
            }
        }

        val data = getTournament(id)
        cacheLock.withLock {
            if (data == null || data.isComplete()) {
                cache.remove(id)
                return data?.let { LiveTournament(it) }
            } else {
                // This entry could exist by now
                val cacheEntry = cache[id]
                if (cacheEntry != null) {
                    cacheEntry.update(data)
                    return cacheEntry
                } else {
                    val new = LiveTournament(data)
                    cache[id] = new
                    return new
                }
            }
        }
    }

    override suspend fun getLiveTournament(id: TournamentId) = getLiveTournament(id, useCache = true)
}
