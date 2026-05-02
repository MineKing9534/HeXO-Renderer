package de.mineking.hexo.api

import de.mineking.hexo.api.game.FinishedGameRepository
import de.mineking.hexo.api.game.FinishedGameRepositoryImpl
import de.mineking.hexo.api.socket.AuthData
import de.mineking.hexo.api.socket.SocketIOClient
import de.mineking.hexo.api.tournament.TournamentRepository
import de.mineking.hexo.api.tournament.TournamentRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class InternalHexoApi

const val HEXO_WEBSITE = "https://hexo.did.science"

expect val DefaultHttpEngine: HttpClientEngine

fun createDefaultHttpClient(
    engine: HttpClientEngine = DefaultHttpEngine,
    config: HttpClientConfig<*>.() -> Unit = {},
) = HttpClient(engine) {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(HexoApiClient.json)
    }

    install(ContentNegotiation) {
        json(HexoApiClient.json)
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
    }

    config()
}

class HexoApiClient(
    internal val coroutineScope: CoroutineScope,
    private val host: String = HEXO_WEBSITE,
    connectSocketIO: Boolean = false,
    private val httpClient: HttpClient = createDefaultHttpClient(),
) {
    companion object {
        internal val json = Json {
            ignoreUnknownKeys = true
        }
    }

    private val sockerIO = if (connectSocketIO) createSocketIOClient(host) else null
    val events = sockerIO?.events ?: MutableSharedFlow()

    val finishedGameRepository: FinishedGameRepository = FinishedGameRepositoryImpl(this)
    val tournamentRepository: TournamentRepository = TournamentRepositoryImpl(this)

    internal suspend fun request(path: String, builder: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
        httpClient.request("$host/api$path", builder)

    private fun createSocketIOClient(host: String): SocketIOClient {
        val authData = AuthData(
            deviceId = Uuid.random().toString(),
            ephemeralClientId = Uuid.random().toString(),
            versionHash = "HeXO-Kotlin",
        )

        return SocketIOClient(json, host, authData)
    }
}
