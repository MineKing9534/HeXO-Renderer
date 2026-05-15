package de.mineking.hexo.bot

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
import io.ktor.server.util.getOrFail
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class HttpServer(
    private val discordUserAuthenticationRepository: DiscordUserAuthenticationRepository,
    private val linkedRolesUpdateService: LinkedRolesUpdateService?,
    port: Int,
) {
    private val signer = OAuth2Signer(Random.nextBytes(32))
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
                    state = signer.generateState(),
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
        val code = call.queryParameters.getOrFail("code")
        val state = call.queryParameters.getOrFail("state")

        if (!signer.verifyState(state)) {
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

private class OAuth2Signer(secret: ByteArray) {
    private val key = SecretKeySpec(secret, "HmacSHA256")
    private val encoder = Base64.getUrlEncoder()

    fun generateState(): String {
        val expiry = (Clock.System.now() + 10.minutes).epochSeconds
        val nonce = encoder.encodeToString(Random.nextBytes(16))
        val payload = "$expiry.$nonce"
        val sign = payload.sign()
        return "$payload.$sign"
    }

    fun verifyState(state: String): Boolean {
        val parts = state.split(".")
        if (parts.size != 3) return false

        val (expiry, nonce, sign) = parts
        val payload = "$expiry.$nonce"

        if (payload.sign() != sign) return false
        return Clock.System.now().epochSeconds <= expiry.toLong()
    }

    private fun String.sign(): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        return encoder.encodeToString(mac.doFinal(toByteArray()))
    }
}
