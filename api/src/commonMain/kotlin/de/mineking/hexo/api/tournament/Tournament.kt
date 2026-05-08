package de.mineking.hexo.api.tournament

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.InternalHexoApi
import de.mineking.hexo.api.ProfileId
import de.mineking.hexo.api.game.GameReference
import de.mineking.hexo.api.game.SessionId
import de.mineking.hexo.api.utils.Instant
import de.mineking.hexo.api.utils.TimeControl
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@JvmInline
@Serializable
value class TournamentId(val value: Uuid)

@JvmInline
@Serializable
value class TournamentMatchId(val value: String)

class Tournament(
    @property:InternalHexoApi val client: HexoApiClient,
    val id: TournamentId,
    val name: String,
    val description: String?,
    val format: TournamentFormat,
    val status: TournamentStatus,
    val scheduledStartAt: Instant,
    val checkInOpensAt: Instant,
    val checkInClosesAt: Instant,
    val maxPlayers: Int,
    val registeredCount: Int,
    val checkedInCount: Int,
    val timeControl: TimeControl,
    val participants: List<TournamentParticipant>,
    val matches: List<TournamentMatch>,
) {
    companion object {
        private fun TournamentDto.createParticipantList(): List<TournamentParticipant> {
            val participantsDtos = participants.associateBy { it.profileId }
            return standings.map {
                val participant = participantsDtos[it.profileId] ?: error("Couldn't find participant ${it.profileId}")
                TournamentParticipant(
                    profileId = participant.profileId,
                    displayName = participant.displayName,
                    image = participant.image,
                    registeredAt = participant.registeredAt,
                    seed = participant.seed,
                    standing = it,
                )
            }.sortedBy { it.standing.rank }
        }

        private fun TournamentDto.createMatchList(client: HexoApiClient, participants: List<TournamentParticipant>): List<TournamentMatch> {
            val participantsById = participants.associateBy { it.profileId }

            return matches.map { match ->
                val players = match.slots.mapIndexed { index, slot ->
                    TournamentMatchPlayer(
                        participant = participantsById[slot.profileId],
                        isByte = slot.isBye,
                        wins = when (index) {
                            0 -> match.leftWins
                            1 -> match.rightWins
                            else -> error("Unexpected slot index $index")
                        },
                        isWinner = slot.profileId == match.winnerProfileId,
                        seed = slot.seed,
                    )
                }

                TournamentMatch(
                    id = match.id,
                    bracket = match.bracket,
                    round = match.round,
                    order = match.order,
                    state = match.state,
                    bestOf = match.bestOf,
                    resultType = match.resultType,
                    waitingForPlayers = match.waitingForPlayers,
                    startedAt = match.startedAt,
                    resolvedAt = match.resolvedAt,
                    pastGames = match.gameIds.map { GameReference(client, it) },
                    sessionId = match.sessionId,
                    players = players,
                    winner = participantsById[match.winnerProfileId],
                )
            }
        }

        internal fun of(client: HexoApiClient, dto: TournamentDto): Tournament {
            val participants = dto.createParticipantList()
            val matches = dto.createMatchList(client, participants)

            return Tournament(
                client = client,
                id = dto.id,
                name = dto.name,
                description = dto.description,
                format = TournamentFormat.of(dto, matches),
                status = dto.status,
                scheduledStartAt = dto.scheduledStartAt,
                checkInOpensAt = dto.checkInOpensAt,
                checkInClosesAt = dto.checkInClosesAt,
                maxPlayers = dto.maxPlayers,
                registeredCount = dto.registeredCount,
                checkedInCount = dto.checkedInCount,
                timeControl = dto.timeControl,
                participants = participants,
                matches = matches,
            )
        }
    }
}

class TournamentParticipant(
    val profileId: ProfileId,
    val displayName: String,
    val image: String?,
    val registeredAt: Instant,
    val seed: Int?,
    val standing: TournamentStanding,
)

data class TournamentMatchPlayer(
    val participant: TournamentParticipant?,
    val seed: Int?,
    val isByte: Boolean,
    val wins: Int,
    val isWinner: Boolean,
)

// TODO allow fetching current session
class TournamentMatch(
    val id: TournamentMatchId,
    val bracket: TournamentBracket,
    val round: Int,
    val order: Int,
    val state: TournamentMatchState,
    val bestOf: Int,
    val winner: TournamentParticipant?,
    val resultType: TournamentMatchResultType?,
    val waitingForPlayers: Boolean,
    val startedAt: Instant?,
    val resolvedAt: Instant?,
    val players: List<TournamentMatchPlayer>,
    val pastGames: List<GameReference>,
    val sessionId: SessionId?,
)
