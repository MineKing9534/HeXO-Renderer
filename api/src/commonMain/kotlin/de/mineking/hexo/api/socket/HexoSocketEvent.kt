package de.mineking.hexo.api.socket

import de.mineking.hexo.api.game.SessionId
import de.mineking.hexo.api.tournament.TournamentId
import de.mineking.hexo.api.utils.Instant
import de.mineking.hexo.api.utils.TimeControl
import kotlinx.serialization.Serializable

sealed interface HexoSocketEvent {
    companion object {
        internal val eventMappings = mapOf(
            "tournament-updated" to TournamentUpdate::class,
            "lobby-updated" to LobbyUpdate::class,
            "lobby-removed" to LobbyRemove::class,
        )
    }

    @Serializable
    data class TournamentUpdate(
        val tournamentId: TournamentId,
        val updatedAt: Instant,
    ) : HexoSocketEvent

    @Serializable
    data class LobbyUpdate(
        val id: SessionId,
        val timeControl: TimeControl,
        val rated: Boolean,
        val createdAt: Instant,
        val startedAt: Instant?,
    ) : HexoSocketEvent

    @Serializable
    data class LobbyRemove(
        val id: SessionId,
    ) : HexoSocketEvent
}
