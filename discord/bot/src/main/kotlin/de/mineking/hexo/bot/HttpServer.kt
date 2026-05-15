package de.mineking.hexo.bot

import de.mineking.hexo.link.oauth2.DiscordUserAuthenticationRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail

class HttpServer(port: Int, private val discordUserAuthenticationRepository: DiscordUserAuthenticationRepository) {
    private val logger = KotlinLogging.logger {}

    private val server = embeddedServer(Netty, port = port) {
        install(StatusPages) {
            exception<BadRequestException> { call, cause ->
                call.respond(HttpStatusCode.BadRequest, cause.message ?: "")
            }

            exception<Exception> { call, cause ->
                call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
                logger.error(cause) { "Unexpected error in http handler" }
            }
        }

        routing {
            get("/oauth2/callback") {
                handleOAuth2Callback()
            }
        }
    }

    private suspend fun RoutingContext.handleOAuth2Callback() {
        val code = call.queryParameters.getOrFail("code")
        val result = discordUserAuthenticationRepository.authenticateUser(code)

        if (result == null) {
            call.respond(HttpStatusCode.Unauthorized, "Failed")
        } else {
            call.respond(HttpStatusCode.OK, "Ok")
        }
    }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }
}
