package de.mineking.hexo.hds.socket

import de.mineking.hexo.hds.session.SessionId
import kotlinx.serialization.Serializable

sealed interface SocketRequest

sealed interface HexoSocketRequest : SocketRequest {
    @Serializable
    @SocketEventName("watch-session")
    data class WatchSession(val sessionId: SessionId) : HexoSocketRequest

    @Serializable
    @SocketEventName("unwatch-session")
    data class UnwatchSession(val sessionId: SessionId) : HexoSocketRequest
}

val SocketRequest.requestName get() = SocketRequestRegistry.requestNames[this::class]
    ?: error("requestName for ${this::class} not found, this should never happen")
