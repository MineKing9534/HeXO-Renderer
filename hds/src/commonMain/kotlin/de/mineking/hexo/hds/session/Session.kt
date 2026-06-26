package de.mineking.hexo.hds.session

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.game.Game
import de.mineking.hexo.hds.game.GameId
import de.mineking.hexo.hds.game.GameMove
import de.mineking.hexo.hds.game.GameOptions
import de.mineking.hexo.hds.game.GameResult
import de.mineking.hexo.hds.game.GameVisibility
import de.mineking.hexo.hds.game.Player
import de.mineking.hexo.hds.game.PlayerId
import de.mineking.hexo.hds.game.TournamentMatchSnapshot
import de.mineking.hexo.hds.profile.ProfileId
import de.mineking.hexo.hds.profile.ProfileRepository
import de.mineking.hexo.hds.tournament.TournamentRepository
import de.mineking.hexo.hds.utils.EntityState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Instant

@JvmInline
@Serializable
value class SessionId(val value: String)

class SessionReference(
    private val repository: SessionRepository,
    val id: SessionId,
) {
    fun observe() = repository.observeSession(id)
}

interface SessionPlayer {
    val profileId: ProfileId?
    val displayName: String
    val elo: Int
}

fun SessionPlayer.isGuest() = profileId != null

interface Session {
    val id: SessionId
    val gameOptions: GameOptions
    val players: List<SessionPlayer>

    fun observe(): SharedFlow<EntityState<Session>>
}

class LobbySession(
    private val repository: SessionRepository,
    override val id: SessionId,
    override val gameOptions: GameOptions,
    override val players: List<SessionPlayer>,
    val createdAt: Instant,
    val startedAt: Instant?,
) : Session {
    override fun observe() = repository.observeSession(id)

    companion object {
        internal fun of(repository: SessionRepository, dto: LobbyInfoDto) = LobbySession(
            repository = repository,
            id = dto.id,
            gameOptions = GameOptions(
                rated = dto.rated,
                timeControl = dto.timeControl,
                visibility = GameVisibility.Public,
            ),
            players = dto.players,
            createdAt = dto.createdAt,
            startedAt = dto.startedAt,
        )
    }
}

fun LobbySession.hasStarted() = startedAt != null

class LiveSession private constructor(
    private val repository: SessionRepository,
    override val id: SessionId,
    override val gameOptions: GameOptions,
    override val players: List<LiveSessionPlayer>,
    val tournamentInfo: TournamentMatchSnapshot?,
    val state: SessionState,
    val game: SessionGame,
    internal val dto: SessionDto,
    internal val lastState: Pair<Instant, SessionStateDto.InGame>?,
    internal val gameState: SessionGameStateDto,
) : Session {
    override fun observe() = repository.observeSession(id)

    companion object {
        private fun SessionDto.createPlayerList(
            repository: ProfileRepository,
            gameState: SessionGameStateDto?,
        ) = players.mapIndexed { index, data ->
            val owner = gameState?.playerTiles?.get(data.id)?.color?.let {
                when {
                    it.red > 200 -> CellOwner.X
                    it.blue > 200 -> CellOwner.O
                    else -> error("Unrecognized color '${it.format()}'")
                }
            } ?: CellOwner.entries[index]

            LiveSessionPlayer(
                repository = repository,
                playerId = data.id,
                profileId = data.profileId?.takeIf { it.value != data.id.value },
                displayName = data.displayName,
                elo = data.rating.eloScore,
                eloAdjustment = data.ratingAdjustment?.let {
                    SessionPlayerEloAdjustment(eloGain = it.eloGain, eloLoss = it.eloLoss)
                },
                color = owner,
                tournamentMatchWins = tournament?.let {
                    when (data.profileId) {
                        it.leftProfileId -> it.leftWins
                        it.rightProfileId -> it.rightWins
                        else -> error("Inconsistent tournament snapshot")
                    }
                },
                timeRemaining = gameState?.playerTimeRemaining[data.id],
                connectionStatus = data.connection.status,
            )
        }.sortedBy { player -> gameState?.cells?.indexOfFirst { it.occupiedBy == player.playerId } }

        private fun SessionStateDto.toSessionState(
            gameState: SessionGameStateDto?,
            playersById: Map<PlayerId, LiveSessionPlayer>,
        ) = when (this) {
            is SessionStateDto.Lobby -> error("Cannot create live LiveSession from lobby")
            is SessionStateDto.InGame -> SessionState.InGame(
                currentTurn = SessionTurn(
                    player = playersById[gameState!!.currentTurnPlayerId]!!,
                    placementsRemaining = gameState.placementsRemaining,
                    expiresIn = gameState.currentTurnExpiresIn,
                ),
            )

            is SessionStateDto.Finished -> SessionState.Finished(
                rematchAcceptedPlayers = rematchAcceptedPlayerIds.mapNotNull { playersById[it] },
            )
        }

        internal fun of(
            client: HdsApiClient,
            repository: SessionRepository,
            dto: SessionDto,
            lastState: Pair<Instant, SessionStateDto.InGame>?,
            gameState: SessionGameStateDto,
            profileRepository: ProfileRepository,
            tournamentRepository: () -> TournamentRepository,
        ): LiveSession {
            val players = dto.createPlayerList(profileRepository, gameState)
            val playersById = players.associateBy { it.playerId }

            val tournament = dto.tournament?.let { TournamentMatchSnapshot.of(it, client.host, tournamentRepository) }

            return LiveSession(
                repository = repository,
                id = dto.id,
                gameOptions = dto.gameOptions,
                players = players,
                tournamentInfo = tournament,
                state = dto.state.toSessionState(gameState, playersById),
                game = SessionGame.of(
                    dto = dto,
                    lastState = lastState,
                    tournamentInfo = tournament,
                    gameState = gameState,
                    players = players,
                    playersById = playersById,
                ),
                dto = dto,
                lastState = lastState,
                gameState = gameState,
            )
        }
    }
}

enum class SessionPlayerConnectionStatus {
    Connected,
    Orphaned,
    Disconnected,
}

data class SessionPlayerEloAdjustment(
    val eloGain: Int,
    val eloLoss: Int,
)

class LiveSessionPlayer(
    repository: ProfileRepository,
    playerId: PlayerId,
    profileId: ProfileId?,
    displayName: String,
    elo: Int,
    val eloAdjustment: SessionPlayerEloAdjustment?,
    color: CellOwner,
    tournamentMatchWins: Int?,
    val timeRemaining: Duration?,
    val connectionStatus: SessionPlayerConnectionStatus,
) : SessionPlayer, Player(
    repository = repository,
    playerId = playerId,
    profileId = profileId,
    displayName = displayName,
    elo = elo,
    color = color,
    tournamentMatchWins = tournamentMatchWins,
) {
    override val profileId = super.profileId
    override val displayName = super.displayName
    override val elo = super.elo
}

sealed interface SessionState {
    data class InGame(val currentTurn: SessionTurn) : SessionState
    data class Finished(val rematchAcceptedPlayers: List<SessionPlayer>) : SessionState
}

data class SessionTurn(
    val player: LiveSessionPlayer,
    val placementsRemaining: Int,
    val expiresIn: Duration?,
)

class SessionGame(
    override val id: GameId,
    override val startedAt: Instant,
    override val result: GameResult?,
    override val options: GameOptions,
    override val tournamentInfo: TournamentMatchSnapshot?,
    override val moves: List<GameMove>,
    override val players: List<LiveSessionPlayer>,
) : Game {
    override val moveCount get() = moves.size

    companion object {
        internal fun of(
            dto: SessionDto,
            lastState: Pair<Instant, SessionStateDto.InGame>?,
            tournamentInfo: TournamentMatchSnapshot?,
            gameState: SessionGameStateDto,
            players: List<LiveSessionPlayer>,
            playersById: Map<PlayerId, LiveSessionPlayer>,
        ) = SessionGame(
            id = (dto.state as SessionStateDto.GameSessionState).gameId,
            startedAt = when (dto.state) {
                is SessionStateDto.InGame -> dto.state.startedAt
                is SessionStateDto.Finished -> lastState!!.second.startedAt
            },
            result = when (dto.state) {
                is SessionStateDto.Finished -> GameResult(
                    winner = playersById[dto.state.winningPlayerId],
                    duration = lastState!!.first - lastState.second.startedAt,
                    reason = dto.state.finishReason,
                )
                else -> null
            },
            options = dto.gameOptions,
            tournamentInfo = tournamentInfo,
            moves = gameState.cells?.map {
                GameMove(
                    coordinate = CellCoordinate(it.q, it.r),
                    player = playersById[it.occupiedBy]!!,
                )
            }!!,
            players = players,
        )
    }
}
