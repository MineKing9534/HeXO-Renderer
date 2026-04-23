package de.mineking.hexo.history

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

interface MatchRepository {
    suspend fun getGame(id: Uuid): Match?
}

fun MatchRepository.cached(cacheSize: Long = 16): MatchRepository = when (this) {
    is CachingMatchRepository -> this
    else -> CachingMatchRepository(this, cacheSize)
}

fun MatchRepository(): MatchRepository = MatchRepositoryImpl(CIO.create(), "https://hexo.did.science/api")

private class MatchRepositoryImpl(engine: HttpClientEngine, private val host: String) : MatchRepository {
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    override suspend fun getGame(id: Uuid): Match? {
        val response = client.get("$host/finished-games/$id")

        if (!response.status.isSuccess()) return null
        return response.body()
    }
}

private class CachingMatchRepository(val delegate: MatchRepository, cacheSize: Long) : MatchRepository {
    private val cache = Caffeine.newBuilder().maximumSize(cacheSize).asCache<Uuid, Match>()

    override suspend fun getGame(id: Uuid) = cache.getOrNull(id) { delegate.getGame(it) }
}
