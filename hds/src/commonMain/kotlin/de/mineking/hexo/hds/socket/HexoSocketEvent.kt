package de.mineking.hexo.hds.socket

import de.mineking.hexo.hds.session.SessionDto
import de.mineking.hexo.hds.session.SessionGameStateDto
import de.mineking.hexo.hds.session.SessionId
import de.mineking.hexo.hds.session.SessionMoveDto
import de.mineking.hexo.hds.session.SessionPlayerDto
import de.mineking.hexo.hds.session.SessionStateDto
import de.mineking.hexo.hds.tournament.TournamentId
import de.mineking.hexo.hds.utils.Instant
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

internal annotation class SocketEventName(val name: String)

sealed interface SocketEvent

sealed interface HexoSocketEvent : SocketEvent

@Serializable
@SocketEventName("tournament-updated")
internal data class TournamentUpdate(
    val tournamentId: TournamentId,
    val updatedAt: Instant,
) : HexoSocketEvent

@Serializable
@SocketEventName("session-watch-started")
internal data class SessionWatchStarted(
    val session: SessionDto,
    val gameState: SessionGameStateDto,
) : HexoSocketEvent

@Serializable
@SocketEventName("session-watch-error")
internal data class SessionWatchError(
    val sessionId: SessionId,
    val message: String,
) : HexoSocketEvent

@Serializable
@SocketEventName("session-updated")
internal data class SessionUpdated(
    val sessionId: SessionId,
    val session: PartialSessionDto,
) : HexoSocketEvent {
    @Serializable
    data class PartialSessionDto(
        val state: SessionStateDto? = null,
        val players: List<SessionPlayerDto>? = null,
    )
}

@Serializable
@SocketEventName("game-state")
internal data class GameStateUpdated(
    val sessionId: SessionId,
    val gameState: SessionGameStateDto,
) : HexoSocketEvent

@Serializable
@SocketEventName("game-cell-place")
internal data class GameCellPlace(
    val sessionId: SessionId,
    val state: SessionGameStateDto,
    val cell: SessionMoveDto,
) : HexoSocketEvent

sealed interface ProtocolSocketEvent : SocketEvent {
    @Serializable
    @SocketEventName("connect")
    data object Connected : ProtocolSocketEvent

    @Serializable
    @SocketEventName("disconnect")
    data class Disconnected(val message: String? = null) : ProtocolSocketEvent

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

val KClass<out SocketEvent>.eventName get() = SocketEventRegistry.eventNames[this]
    ?: error("eventName for $this not found, this should never happen")
