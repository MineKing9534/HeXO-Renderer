package de.mineking.hexo.api

import de.mineking.hexo.api.game.FinishedGameRepository
import de.mineking.hexo.api.game.FinishedGameRepositoryImpl
import de.mineking.hexo.api.socket.AuthData
import de.mineking.hexo.api.socket.SocketIOClient
import de.mineking.hexo.api.tournament.TournamentRepository
import de.mineking.hexo.api.tournament.TournamentRepositoryImpl
import io.ktor.client.HttpClient
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

const val HEXO_WEBSITE = "https://hexo.did.science"

expect val DefaultHttpEngine: HttpClientEngine

class HexoApiClient(
    internal val coroutineScope: CoroutineScope,
    private val host: String = HEXO_WEBSITE,
    connectSocketIO: Boolean = false,
    engine: HttpClientEngine = DefaultHttpEngine,
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }

    private val sockerIO = if (connectSocketIO) createSocketIOClient(host) else null
    val events = sockerIO?.events ?: MutableSharedFlow()

    val finishedGameRepository: FinishedGameRepository = FinishedGameRepositoryImpl(this)
    val tournamentRepository: TournamentRepository = TournamentRepositoryImpl(this)

    private val client = HttpClient(engine) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
        }

        install(ContentNegotiation) {
            json(json)
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    internal suspend fun request(path: String, builder: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
        client.request("$host/api$path", builder)

    private fun createSocketIOClient(host: String): SocketIOClient {
        val authData = AuthData(
            deviceId = Uuid.random().toString(),
            ephemeralClientId = Uuid.random().toString(),
            versionHash = "HeXO-Kotlin",
        )

        return SocketIOClient(json, host, authData)
    }
}
