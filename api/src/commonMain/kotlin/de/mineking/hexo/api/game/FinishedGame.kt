package de.mineking.hexo.api.game

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.InternalHexoApi
import de.mineking.hexo.api.profile.ProfileId
import de.mineking.hexo.api.profile.ProfileRepository
import de.mineking.hexo.api.tournament.TournamentBracket
import de.mineking.hexo.api.tournament.TournamentId
import de.mineking.hexo.api.tournament.TournamentMatchId
import de.mineking.hexo.api.tournament.TournamentRepository
import de.mineking.hexo.api.utils.Duration
import de.mineking.hexo.core.CellOwner
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class PlayerId(val value: String)

@JvmInline
@Serializable
value class GameId(val value: String)

class GameReference(
    private val repository: FinishedGameRepository,
    val id: GameId,
) {
    @OptIn(InternalHexoApi::class)
    suspend fun retrieveGame() = repository.getGame(id)
}

class FinishedGame(
    @property:InternalHexoApi val client: HexoApiClient,
    val id: GameId,
    val url: String,
    val result: GameResult,
    val options: GameOptions,
    val tournamentInfo: TournamentMatchSnapshot?,
    val moves: List<Move>,
    val moveCount: Int,
    val players: List<Player>,
) {
    companion object {
        private fun FinishedGameDto.createPlayerList(repository: ProfileRepository) = players.map { data ->
            val color = playerTiles[data.playerId]?.color ?: error("Player tile for ${data.playerId} not defined")
            val owner = when {
                color.red > 200 -> CellOwner.X
                color.blue > 200 -> CellOwner.O
                else -> error("Unrecognized color '${color.format()}'")
            }

            Player(
                repository = repository,
                playerId = data.playerId,
                profileId = data.profileId.takeIf { it.value != data.playerId.value },
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

        private fun TournamentMatchSnapshotDto.toTournamentMatchSnapshot(
            host: String,
            repository: () -> TournamentRepository,
        ) = TournamentMatchSnapshot(
            repository = repository,
            tournamentId = tournamentId,
            tournamentUrl = "$host/tournaments/${tournamentId.value}",
            tournamentName = tournamentName,
            matchId = matchId,
            bracket = bracket,
            round = round,
            order = order,
            bestOf = bestOf,
            currentGameNumber = currentGameNumber,
        )

        internal fun of(
            client: HexoApiClient,
            profileRepository: ProfileRepository,
            tournamentRepository: () -> TournamentRepository,
            dto: FinishedGameDto,
        ): FinishedGame {
            val players = dto.createPlayerList(profileRepository)
            val playersById = players.associateBy { it.playerId }

            return FinishedGame(
                client = client,
                id = dto.id,
                url = "${client.host}/finished-games/${dto.id.value}",
                result = GameResult(playersById[dto.result.winningPlayerId], dto.result.duration, dto.result.reason),
                options = dto.options,
                tournamentInfo = dto.tournament?.toTournamentMatchSnapshot(client.host, tournamentRepository),
                moves = dto.moves.map { Move(playersById[it.playerId]!!, it.q, it.r) },
                moveCount = dto.moveCount,
                players = players,
            )
        }
    }
}

class Player(
    private val repository: ProfileRepository,
    val playerId: PlayerId,
    val profileId: ProfileId?,
    val displayName: String,
    val elo: Int,
    val eloChange: Int?,
    val color: CellOwner,
    val tournamentMatchWins: Int?,
    val isWinner: Boolean,
) {
    @OptIn(InternalHexoApi::class)
    suspend fun fetchProfile() = profileId?.let { repository.getProfile(it) }
}

fun Player.isGuest() = profileId == null

class TournamentMatchSnapshot(
    private val repository: () -> TournamentRepository,
    val tournamentId: TournamentId,
    val tournamentUrl: String,
    val tournamentName: String,
    val matchId: TournamentMatchId,
    val bracket: TournamentBracket,
    val round: Int,
    val order: Int,
    val bestOf: Int,
    val currentGameNumber: Int,
) {
    @OptIn(InternalHexoApi::class)
    suspend fun retrieveTournament() = repository().getTournament(tournamentId)

    @OptIn(InternalHexoApi::class)
    fun observeTournament() = repository().observeTournament(tournamentId)
}

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
