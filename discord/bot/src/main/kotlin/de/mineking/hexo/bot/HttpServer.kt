package de.mineking.hexo.bot

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import de.mineking.hexo.bot.utils.LinkedRolesUpdateService
import de.mineking.hexo.link.oauth2.DiscordUserAuthenticationRepository
import de.mineking.hexo.link.oauth2.Scope
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondResource
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.security.SecureRandom
import kotlin.time.Duration.Companion.minutes

class HttpServer(
    private val discordUserAuthenticationRepository: DiscordUserAuthenticationRepository,
    private val linkedRolesUpdateService: LinkedRolesUpdateService?,
    port: Int,
) {
    private val stateManager = StateManager()
    private val logger = KotlinLogging.logger {}

    private val server = embeddedServer(Netty, port = port) {
        install(StatusPages) {
            exception<Exception> { call, cause ->
                call.respondResource("response/error.html")

                if (cause !is BadRequestException) {
                    logger.error(cause) { "Unexpected error in http handler" }
                }
            }

            status(HttpStatusCode.NotFound) {
                call.respondResource("response/not-found.html")
            }
        }

        routing {
            get("/linked-roles") {
                val url = discordUserAuthenticationRepository.discordOAuth2Client.generateAuthorizationUrl(
                    scopes = arrayOf(Scope.Identify, Scope.RoleConnectionsWrite),
                    state = stateManager.getState(),
                )
                call.respondRedirect(url)
            }

            get("/oauth2/callback") {
                handleOAuth2Callback()
            }
        }
    }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }

    private suspend fun RoutingContext.handleOAuth2Callback() {
        val code = call.queryParameters["code"]
        val state = call.queryParameters["state"]

        if (code == null || state == null) {
            call.respondResource("response/oauth2/failed.html")
            return
        }

        if (!stateManager.verifyState(state)) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid State")
            return
        }

        val userId = discordUserAuthenticationRepository.authenticateUser(code)

        if (userId == null) {
            call.respondResource("response/oauth2/failed.html")
        } else {
            call.respondResource("response/oauth2/success.html")
            linkedRolesUpdateService?.scheduleLinkedRoleDataUpdate(userId)
        }
    }
}

private class StateManager {
    private val random = SecureRandom()

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(5.minutes)
        .build<String, Unit>()

    fun getState(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)

        val state = bytes.toHexString()
        cache.put(state, Unit)
        return state
    }

    fun verifyState(state: String): Boolean {
        val result = cache.getIfPresent(state) != null
        cache.invalidate(state)
        return result
    }
}
