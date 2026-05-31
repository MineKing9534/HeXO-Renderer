package de.mineking.hexo.server

import io.ktor.server.routing.Route

interface WebService {
    fun Route.registerRoutes()
}
