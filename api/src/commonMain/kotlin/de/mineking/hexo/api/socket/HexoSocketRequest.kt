package de.mineking.hexo.api.socket

import de.mineking.hexo.api.session.SessionId
import kotlinx.serialization.Serializable

sealed interface SocketRequest

sealed interface HexoSocketRequest : SocketRequest {
    @Serializable
    @SocketEventName("watch-session")
    data class WatchSession(val sessionId: SessionId) : HexoSocketRequest
}

val SocketRequest.requestName get() = SocketRequestRegistry.requestNames[this::class]
    ?: error("requestName for ${this::class} not found, this should never happen")
