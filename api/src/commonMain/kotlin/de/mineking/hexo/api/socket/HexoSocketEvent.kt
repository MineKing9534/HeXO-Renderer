package de.mineking.hexo.api.socket

import de.mineking.hexo.api.game.SessionId
import de.mineking.hexo.api.tournament.TournamentId
import de.mineking.hexo.api.utils.Instant
import de.mineking.hexo.api.utils.TimeControl
import kotlinx.serialization.Serializable

internal annotation class SocketEventName(val name: String)

sealed interface SocketEvent

sealed interface HexoSocketEvent : SocketEvent {
    @Serializable
    @SocketEventName("tournament-updated")
    data class TournamentUpdate(
        val tournamentId: TournamentId,
        val updatedAt: Instant,
    ) : HexoSocketEvent

    @Serializable
    @SocketEventName("lobby-updated")
    data class LobbyUpdate(
        val id: SessionId,
        val timeControl: TimeControl,
        val rated: Boolean,
        val createdAt: Instant,
        val startedAt: Instant?,
    ) : HexoSocketEvent

    @Serializable
    @SocketEventName("lobby-removed")
    data class LobbyRemove(
        val id: SessionId,
    ) : HexoSocketEvent
}

sealed interface ProtocolSocketEvent : SocketEvent {
    @Serializable
    @SocketEventName("connect")
    data object Connected : ProtocolSocketEvent

    @Serializable
    @SocketEventName("disconnect")
    data object Disconnected : ProtocolSocketEvent

    @Serializable
    @SocketEventName("reconnect")
    data object Reconnected : ProtocolSocketEvent

    @Serializable
    @SocketEventName("error")
    data class Error(val message: String) : ProtocolSocketEvent

    @Serializable
    @SocketEventName("connect_error")
    data class ConnectError(val message: String) : ProtocolSocketEvent
}
