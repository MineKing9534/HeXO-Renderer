package de.mineking.hexo.api

import de.mineking.hexo.api.socket.HexoSocketClient
import de.mineking.hexo.api.utils.EntityRequesterFactory
import de.mineking.hexo.api.utils.logRequestErrors
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json

const val DEFAULT_HOST = "https://hexo.did.science"

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class InternalHexoApi

private val logger = KotlinLogging.logger {}

internal val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

class HexoApiClient(
    internal val coroutineScope: CoroutineScope = createCoroutineScope(logger),
    internal val host: String = DEFAULT_HOST,
    internal val socketClient: HexoSocketClient?,
    private val httpClient: HttpClient = createDefaultHttpClient(),
    internal val entityRequesterFactory: EntityRequesterFactory = EntityRequesterFactory.Debouncing(coroutineScope).logRequestErrors(),
) {
    internal suspend fun request(path: String, builder: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
        httpClient.request("$host/api$path", builder)

    fun shutdown() {
        coroutineScope.cancel()
        socketClient?.client?.disconnect()
    }
}

expect val DefaultHttpEngine: HttpClientEngine
expect val DefaultCoroutineDispatcher: CoroutineDispatcher

expect val HEXO_USER_AGENT: String?

fun createDefaultHttpClient(
    engine: HttpClientEngine = DefaultHttpEngine,
    config: HttpClientConfig<*>.() -> Unit = {},
) = HttpClient(engine) {
    install(ContentNegotiation) {
        json(json)
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.UserAgent, HEXO_USER_AGENT)
    }

    config()
}

fun createCoroutineScope(logger: KLogger, dispatcher: CoroutineDispatcher = DefaultCoroutineDispatcher): CoroutineScope {
    val parent = SupervisorJob()
    return CoroutineScope(dispatcher + parent + CoroutineExceptionHandler { _, throwable ->
        logger.error(throwable) { "Uncaught exception from coroutine" }
        if (throwable is Error) {
            parent.cancel()
            throw throwable
        }
    })
}
