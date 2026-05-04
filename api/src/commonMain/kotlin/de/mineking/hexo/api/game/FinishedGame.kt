package de.mineking.hexo.api.game

import de.mineking.hexo.api.HEXO_WEBSITE
import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.InternalHexoApi
import de.mineking.hexo.api.ProfileId
import de.mineking.hexo.api.tournament.TournamentBracket
import de.mineking.hexo.api.tournament.TournamentId
import de.mineking.hexo.api.tournament.TournamentMatchId
import de.mineking.hexo.api.utils.Duration
import de.mineking.hexo.core.CellOwner
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@JvmInline
@Serializable
value class PlayerId(val value: String)

@JvmInline
@Serializable
value class GameId(val value: Uuid)

class GameReference(
    @property:InternalHexoApi val client: HexoApiClient,
    val id: GameId,
) {
    @OptIn(InternalHexoApi::class)
    suspend fun retrieveGame() = client.finishedGameRepository.getGame(id)
}

class FinishedGame(
    @property:InternalHexoApi val client: HexoApiClient,
    val id: GameId,
    val result: GameResult,
    val options: GameOptions,
    val tournamentInfo: TournamentMatchSnapshot?,
    val moves: List<Move>,
    val players: List<Player>,
) {
    companion object {
        private fun FinishedGameDto.createPlayerList() = players.map { data ->
            val color = playerTiles[data.playerId]?.color ?: error("Player tile for ${data.playerId} not defined")
            val owner = when {
                color.red > 200 -> CellOwner.X
                color.blue > 200 -> CellOwner.O
                else -> error("Unrecognized color '${color.format()}'")
            }

            Player(
                playerId = data.playerId,
                profileId = data.profileId,
                displayName = data.displayName,
                elo = data.elo,
                eloChange = data.eloChange,
                color = owner,
                isWinner = result.winningPlayerId == data.playerId,
                tournamentMatchWins = tournament?.let {
                    when (data.profileId) {
                        it.leftProfileId -> it.leftWins
                        it.rightProfileId -> it.rightWins
                        else -> error("Inconsistent tournament snapshot")
                    }
                },
            )
        }.sortedBy { player -> moves.indexOfFirst { it.playerId == player.playerId } }

        private fun TournamentMatchSnapshotDto.toTournamentMatchSnapshot(client: HexoApiClient) = TournamentMatchSnapshot(
            client = client,
            tournamentId = tournamentId,
            tournamentName = tournamentName,
            matchId = matchId,
            bracket = bracket,
            round = round,
            order = order,
            bestOf = bestOf,
            currentGameNumber = currentGameNumber,
        )

        internal fun of(client: HexoApiClient, dto: FinishedGameDto): FinishedGame {
            val players = dto.createPlayerList()
            val playersById = players.associateBy { it.playerId }

            return FinishedGame(
                client = client,
                id = dto.id,
                result = GameResult(playersById[dto.result.winningPlayerId], dto.result.duration, dto.result.reason),
                options = dto.options,
                tournamentInfo = dto.tournament?.toTournamentMatchSnapshot(client),
                moves = dto.moves.map { Move(playersById[it.playerId]!!, it.q, it.r) },
                players = players,
            )
        }
    }

    val url get() = "${HEXO_WEBSITE}/games/${id.value}"
}

data class Player(
    val playerId: PlayerId,
    val profileId: ProfileId,
    val displayName: String,
    val elo: Int,
    val eloChange: Int?,
    val color: CellOwner,
    val tournamentMatchWins: Int?,
    val isWinner: Boolean,
)

data class TournamentMatchSnapshot(
    @property:InternalHexoApi val client: HexoApiClient,
    val tournamentId: TournamentId,
    val tournamentName: String,
    val matchId: TournamentMatchId,
    val bracket: TournamentBracket,
    val round: Int,
    val order: Int,
    val bestOf: Int,
    val currentGameNumber: Int,
) {
    val tournamentUrl get() = "${HEXO_WEBSITE}/tournaments/${tournamentId.value}"

    @OptIn(InternalHexoApi::class)
    suspend fun retrieveTournament() = client.tournamentRepository.getTournament(tournamentId)
}

fun Player.isGuest() = profileId.value == playerId.value

data class GameResult(
    val winner: Player?,
    val duration: Duration,
    val reason: GameFinishReason,
)

data class Move(
    val player: Player,
    val q: Int,
    val r: Int,
)
