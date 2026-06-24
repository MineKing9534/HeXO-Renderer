package de.mineking.hexo.api.session

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.profile.ProfileRepository
import de.mineking.hexo.api.socket.GameCellPlace
import de.mineking.hexo.api.socket.GameStateUpdated
import de.mineking.hexo.api.socket.HexoSocketRequest
import de.mineking.hexo.api.socket.ProtocolSocketEvent
import de.mineking.hexo.api.socket.SessionUpdated
import de.mineking.hexo.api.socket.SessionWatchError
import de.mineking.hexo.api.socket.SessionWatchStarted
import de.mineking.hexo.api.socket.SocketIOClient
import de.mineking.hexo.api.socket.listen
import de.mineking.hexo.api.tournament.TournamentRepository
import de.mineking.hexo.api.utils.EntityState
import de.mineking.hexo.api.utils.withLock
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock

private val logger = KotlinLogging.logger {}

interface SessionRepository {
    // TODO track available lobbies

    fun observeSession(id: SessionId): StateFlow<EntityState<Session>>
}

internal class SessionRepositoryImpl(
    private val client: HexoApiClient,
    private val profileRepository: ProfileRepository,
    private val tournamentRepository: () -> TournamentRepository,
) : SessionRepository {
    private val sessionsLock = SynchronizedObject()
    private val sessions = mutableMapOf<SessionId, StateFlow<EntityState<Session>>>()

    private fun Session.createLastState() = dto.state
        .let { it as? SessionStateDto.InGame }
        ?.let { Clock.System.now() to it }
        ?: lastState

    private fun MutableStateFlow<EntityState<Session>>.populate(client: SocketIOClient) {
        client.listen<SessionWatchStarted> { event ->
            logger.info { "Successfully joined session ${event.session.id.value}" }
            value = EntityState.Data(Session.of(
                client = this@SessionRepositoryImpl.client,
                dto = event.session,
                lastState = null,
                gameState = event.gameState,
                profileRepository = profileRepository,
                tournamentRepository = tournamentRepository,
            ))
        }

        client.listen<GameCellPlace> { event ->
            val value = value
            if (value !is EntityState.Data || event.sessionId != value.value.id) {
                logger.warn { "Received game-cell-place event for unconnected session ${event.sessionId.value}" }
                return@listen
            }

            this@populate.value = EntityState.Data(Session.of(
                client = this@SessionRepositoryImpl.client,
                dto = value.value.dto,
                lastState = value.value.createLastState(),
                gameState = event.state.copy(
                    cells = (value.value.gameState.cells ?: emptyList()) + event.cell,
                    playerTiles = value.value.gameState.playerTiles,
                ),
                profileRepository = profileRepository,
                tournamentRepository = tournamentRepository,
            ))
        }

        client.listen<GameStateUpdated> { event ->
            val value = value
            if (value !is EntityState.Data || event.sessionId != value.value.id) {
                logger.warn { "Received game-state event for unconnected session ${event.sessionId.value}" }
                return@listen
            }

            this@populate.value = EntityState.Data(Session.of(
                client = this@SessionRepositoryImpl.client,
                dto = value.value.dto,
                lastState = value.value.createLastState(),
                gameState = event.gameState,
                profileRepository = profileRepository,
                tournamentRepository = tournamentRepository,
            ))
        }
    }

    private fun createSession(client: SocketIOClient, id: SessionId): StateFlow<EntityState<Session>> {
        val flow = MutableStateFlow<EntityState<Session>>(EntityState.Loading)

        fun cleanup() {
            client.disconnect()
            sessionsLock.withLock {
                sessions -= id
            }

            logger.info { "Disconnected SocketIO fork for session ${id.value}" }
        }

        client.listen<ProtocolSocketEvent.Connected> {
            logger.info { "Successfully connected SocketIO fork for session ${id.value}" }

            remove()
            client.request(HexoSocketRequest.WatchSession(id))
        }

        client.listen<ProtocolSocketEvent.ConnectError> {
            logger.error { "Failed to establish SocketIO connection for client fork for session ${id.value}" }

            flow.value = EntityState.NotFound
            cleanup()
        }

        client.listen<ProtocolSocketEvent.Error> {
            logger.error { "Unexpected SocketIO error for client fork for session ${id.value}: ${it.message}" }

            flow.value = EntityState.NotFound
            cleanup()
        }

        client.listen<SessionWatchError> {
            flow.value = EntityState.NotFound
            cleanup()
        }

        client.listen<SessionUpdated> { event ->
            if (event.sessionId != id) return@listen

            val value = flow.value
            if (value !is EntityState.Data) {
                logger.warn { "Received session-updated event for unconnected session ${event.sessionId.value}" }
                return@listen
            }

            val session = Session.of(
                client = this@SessionRepositoryImpl.client,
                dto = value.value.dto.copy(
                    state = event.session.state ?: value.value.dto.state,
                    players = event.session.players ?: value.value.dto.players,
                ),
                lastState = value.value.createLastState(),
                gameState = value.value.gameState,
                profileRepository = profileRepository,
                tournamentRepository = tournamentRepository,
            )

            flow.value = EntityState.Data(session)

            if (event.session.state !is SessionStateDto.Finished) return@listen
            if (session.players.any { it.connectionStatus == SessionPlayerConnectionStatus.Disconnected }) {
                logger.info { "Session ${id.value} removed because it has finished; Disconnecting SocketIO fork..." }
                cleanup()
            }
        }

        flow.populate(client)
        client.connect()

        return flow
    }

    override fun observeSession(id: SessionId): StateFlow<EntityState<Session>> {
        if (client.socketClient == null) error("Cannot observe sessions without a SocketIO connection")

        return sessionsLock.withLock {
            sessions.getOrPut(id) {
                // We need to fork a new client since HDS only allows one session per connection
                createSession(client.socketClient.client.fork(), id)
            }
        }
    }
}
