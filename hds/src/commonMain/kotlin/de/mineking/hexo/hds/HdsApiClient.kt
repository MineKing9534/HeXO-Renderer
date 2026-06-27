package de.mineking.hexo.hds

import de.mineking.hexo.hds.formation.FormationRepository
import de.mineking.hexo.hds.formation.FormationRepositoryImpl
import de.mineking.hexo.hds.game.FinishedGameRepository
import de.mineking.hexo.hds.game.FinishedGameRepositoryImpl
import de.mineking.hexo.hds.leaderboard.LeaderboardRepository
import de.mineking.hexo.hds.leaderboard.LeaderboardRepositoryImpl
import de.mineking.hexo.hds.profile.ProfileRepository
import de.mineking.hexo.hds.profile.ProfileRepositoryImpl
import de.mineking.hexo.hds.session.SessionRepository
import de.mineking.hexo.hds.session.SessionRepositoryImpl
import de.mineking.hexo.hds.socket.HexoSocketClient
import de.mineking.hexo.hds.tournament.TournamentRepository
import de.mineking.hexo.hds.tournament.TournamentRepositoryImpl
import de.mineking.hexo.hds.utils.EntityRequesterFactory
import de.mineking.hexo.hds.utils.logRequestErrors
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

private val logger = KotlinLogging.logger {}

internal val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

interface RepositoryWrapper {
    fun ProfileRepository.wrap(): ProfileRepository
    fun LeaderboardRepository.wrap(): LeaderboardRepository
    fun FinishedGameRepository.wrap(): FinishedGameRepository
    fun TournamentRepository.wrap(): TournamentRepository
    fun FormationRepository.wrap(): FormationRepository
    fun SessionRepository.wrap(): SessionRepository

    companion object : RepositoryWrapper {
        override fun ProfileRepository.wrap() = this
        override fun LeaderboardRepository.wrap() = this
        override fun FinishedGameRepository.wrap() = this
        override fun TournamentRepository.wrap() = this
        override fun FormationRepository.wrap() = this
        override fun SessionRepository.wrap() = this
    }
}

class HdsApiClient(
    internal val coroutineScope: CoroutineScope = createCoroutineScope(logger),
    internal val host: String = DEFAULT_HOST,
    internal val socketClient: HexoSocketClient?,
    private val httpClient: HttpClient = createDefaultHttpClient(),
    internal val entityRequesterFactory: EntityRequesterFactory = EntityRequesterFactory.Debouncing(coroutineScope).logRequestErrors(),
    repositoryWrapper: RepositoryWrapper = RepositoryWrapper,
) {
    internal suspend fun request(path: String, builder: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
        httpClient.request("$host/api$path", builder)

    val formationRepository = repositoryWrapper.run { FormationRepositoryImpl(this@HdsApiClient).wrap() }
    val finishedGameRepository = repositoryWrapper.run { FinishedGameRepositoryImpl(this@HdsApiClient).wrap() }
    val leaderboardRepository = repositoryWrapper.run { LeaderboardRepositoryImpl(this@HdsApiClient).wrap() }
    val profileRepository = repositoryWrapper.run { ProfileRepositoryImpl(this@HdsApiClient).wrap() }
    val sessionRepository = repositoryWrapper.run { SessionRepositoryImpl(this@HdsApiClient).wrap() }
    val tournamentRepository = repositoryWrapper.run { TournamentRepositoryImpl(this@HdsApiClient).wrap() }

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
