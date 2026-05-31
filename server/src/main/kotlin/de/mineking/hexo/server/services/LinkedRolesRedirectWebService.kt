package de.mineking.hexo.server.services

import de.mineking.hexo.link.oauth2.DiscordOAuth2Client
import de.mineking.hexo.link.oauth2.Scope
import de.mineking.hexo.server.WebService
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

class LinkedRolesRedirectWebService(
    private val oAuth2CallbackServer: OAUth2CallbackWebService,
    private val discordOAuth2Client: DiscordOAuth2Client,
) : WebService {
    override fun Route.registerRoutes() {
        get("/linked-roles") {
            val url = discordOAuth2Client.generateAuthorizationUrl(
                scopes = arrayOf(Scope.Identify, Scope.RoleConnectionsWrite),
                state = oAuth2CallbackServer.generateState(),
            )
            call.respondRedirect(url)
        }
    }
}
