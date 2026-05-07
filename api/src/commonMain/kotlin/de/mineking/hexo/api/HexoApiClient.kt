package de.mineking.hexo.api

import de.mineking.hexo.api.game.FinishedGameRepository
import de.mineking.hexo.api.game.FinishedGameRepositoryImpl
import de.mineking.hexo.api.socket.SocketIOClient
import de.mineking.hexo.api.socket.SocketIOOptions
import de.mineking.hexo.api.tournament.TournamentRepository
import de.mineking.hexo.api.tournament.TournamentRepositoryImpl
import io.github.oshai.kotlinlogging.KotlinLogging
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class InternalHexoApi

private val logger = KotlinLogging.logger {}
const val HEXO_WEBSITE = "https://hexo.did.science"

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

class HexoApiClient(
    internal val coroutineScope: CoroutineScope = createCoroutineScope(),
    private val host: String = HEXO_WEBSITE,
    socketIOOptions: SocketIOOptions? = SocketIOOptions.createDefault(host),
    private val httpClient: HttpClient = createDefaultHttpClient(),
) {
    private val sockerIO = socketIOOptions?.let { SocketIOClient(json, it) }
    val events = sockerIO?.events ?: MutableSharedFlow()

    val finishedGameRepository: FinishedGameRepository = FinishedGameRepositoryImpl(this)
    val tournamentRepository: TournamentRepository = TournamentRepositoryImpl(this)

    internal suspend fun request(path: String, builder: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
        httpClient.request("$host/api$path", builder)
}

expect val DefaultHttpEngine: HttpClientEngine
expect val DefaultCoroutineDispatcher: CoroutineDispatcher

fun createDefaultHttpClient(
    engine: HttpClientEngine = DefaultHttpEngine,
    config: HttpClientConfig<*>.() -> Unit = {},
) = HttpClient(engine) {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(json)
    }

    install(ContentNegotiation) {
        json(json)
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
    }

    config()
}

fun createCoroutineScope(dispatcher: CoroutineDispatcher = DefaultCoroutineDispatcher): CoroutineScope {
    val parent = SupervisorJob()
    return CoroutineScope(dispatcher + parent + CoroutineExceptionHandler { _, throwable ->
        logger.error(throwable) { "Uncaught exception from coroutine" }
        if (throwable is Error) {
            parent.cancel()
            throw throwable
        }
    })
}
