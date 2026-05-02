package de.mineking.hexo.api.tournament

import de.mineking.hexo.api.ProfileId
import de.mineking.hexo.api.game.GameId
import de.mineking.hexo.api.utils.Instant
import de.mineking.hexo.api.utils.TimeControl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@JvmInline
@Serializable
value class TournamentId(val value: Uuid)

@Serializable
enum class TournamentFormat {
    @SerialName("single-elimination") SingleElimination,
    @SerialName("double-elimination") DoubleElimination,
    @SerialName("swiss") Swiss,
}

@Serializable
data class Tournament(
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
    val standings: List<TournamentStanding>,
    val matches: List<TournamentMatch>,
    val swissRoundCount: Int?,
)

fun Tournament.isComplete() = status >= TournamentStatus.Completed

@Serializable
enum class TournamentStatus {
    @SerialName("draft") Draft,
    @SerialName("registration-open") RegistrationOpen,
    @SerialName("check-in-open") CheckInOpen,
    @SerialName("waitlist-open") WaitlistOpen,
    @SerialName("live") Live,
    @SerialName("completed") Completed,
    @SerialName("cancelled") Cancelled,
}

@Serializable
data class TournamentParticipant(
    val profileId: ProfileId,
    val displayName: String,
    val image: String?,
    val registeredAt: Instant,
    val seed: Int?,
)

@Serializable
data class TournamentStanding(
    val rank: Int,
    val profileId: ProfileId,
    val wins: Int,
    val losses: Int,
    val buchholz: Int,
    val sonnebornBerger: Int,
)

@Serializable
enum class TournamentMatchState {
    @SerialName("pending") Pending,
    @SerialName("ready") Ready,
    @SerialName("in-progress") InProgress,
    @SerialName("completed") Completed,
}

@Serializable
enum class TournamentMatchResultType {
    @SerialName("played") Played,
    @SerialName("bye") Byte,
    @SerialName("walkover") Walkover,
}

@Serializable
data class TournamentMatchSlot(
    val profileId: ProfileId,
    val displayName: String,
    val image: String?,
    val seed: Int,
    val isBye: Boolean,
)

@Serializable
data class TournamentMatch(
    val round: Int,
    val order: Int,
    val state: TournamentMatchState,
    val bestOf: Int,
    val leftWins: Int,
    val rightWins: Int,
    val winnerProfileId: ProfileId?,
    val loserProfileId: ProfileId?,
    val resultType: TournamentMatchResultType?,
    val waitingForPlayers: Boolean,
    val startedAt: Instant?,
    val resolvedAt: Instant?,
    val slots: List<TournamentMatchSlot>,
    val gameIds: List<GameId>,
)
