package de.mineking.hexo.api.socket

import de.mineking.hexo.api.game.LobbyId
import de.mineking.hexo.api.tournament.TournamentId
import de.mineking.hexo.api.utils.Instant
import de.mineking.hexo.api.utils.TimeControl
import kotlinx.serialization.Serializable

sealed interface HexoEvent {
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
    ) : HexoEvent

    @Serializable
    data class LobbyUpdate(
        val id: LobbyId,
        val timeControl: TimeControl,
        val rated: Boolean,
        val createdAt: Instant,
        val startedAt: Instant?,
    ) : HexoEvent

    @Serializable
    data class LobbyRemove(
        val id: LobbyId,
    ) : HexoEvent
}
