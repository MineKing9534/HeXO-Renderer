package de.mineking.hexo.api.utils

import de.mineking.hexo.api.HexoApiClient
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EntityRequester<K, T>(
    private val client: HexoApiClient,
    private val request: suspend HexoApiClient.(K) -> T?,
) {
    private val waitingLock = Mutex()
    private val waiting = mutableMapOf<K, Deferred<T?>>()

    suspend fun fetch(id: K): T? {
        val deferred = waitingLock.withLock {
            waiting.getOrPut(id) {
                client.coroutineScope.async {
                    client.request(id)
                }
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
}
