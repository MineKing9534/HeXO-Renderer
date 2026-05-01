package de.mineking.hexo.api.tournament

import de.mineking.hexo.api.ProfileId
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
data class Tournament(
    val id: TournamentId,
    val name: String,
    val description: String?,
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
