package de.mineking.hexo.api.tournament

import de.mineking.hexo.api.InternalHexoApi
import de.mineking.hexo.api.ProfileId
import de.mineking.hexo.api.utils.Instant
import de.mineking.hexo.api.utils.TimeControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LiveTournament(data: Tournament) {
    val id = data.id

    @InternalHexoApi
    val raw: StateFlow<Tournament>
        field = MutableStateFlow(data)

    val name: StateFlow<String>
        field = MutableStateFlow(data.name)

    val description: StateFlow<String?>
        field = MutableStateFlow(data.description)

    val status: StateFlow<TournamentStatus>
        field = MutableStateFlow(data.status)

    val scheduledStart: StateFlow<Instant>
        field = MutableStateFlow(data.scheduledStartAt)

    val checkInOpensAt: StateFlow<Instant>
        field = MutableStateFlow(data.checkInOpensAt)

    val checkInClosesAt: StateFlow<Instant>
        field = MutableStateFlow(data.checkInClosesAt)

    val maxPlayers: StateFlow<Int>
        field = MutableStateFlow(data.maxPlayers)

    val registeredCount: StateFlow<Int>
        field = MutableStateFlow(data.registeredCount)

    val timeControl: StateFlow<TimeControl>
        field = MutableStateFlow(data.timeControl)

    val standings: StateFlow<List<LiveTournamentParticipant>>
        field = MutableStateFlow(data.toParticipantData())

    val participants: StateFlow<Map<ProfileId, LiveTournamentParticipant>>
        field = MutableStateFlow(standings.value.associateBy { it.profileId })

    val matchOrganization: StateFlow<TournamentOrganization>
        field = MutableStateFlow(data.toTournamentOrganization())

    @OptIn(InternalHexoApi::class)
    internal fun update(data: Tournament) {
        require(data.id == id)

        raw.value = data

        name.value = data.name
        description.value = data.description
        status.value = data.status
        scheduledStart.value = data.scheduledStartAt
        checkInOpensAt.value = data.checkInOpensAt
        checkInClosesAt.value = data.checkInClosesAt
        maxPlayers.value = data.maxPlayers
        registeredCount.value = data.registeredCount
        timeControl.value = data.timeControl

        standings.value = data.toParticipantData()
        participants.value = standings.value.associateBy { it.profileId }

        matchOrganization.value = data.toTournamentOrganization()
    }

    private fun Tournament.toTournamentOrganization() = when (format) {
        TournamentFormat.Swiss -> SwissTournamentOrganization(this)
        else -> error("Only swiss tournaments are supported by now")
    }

    private fun Tournament.toParticipantData(): List<LiveTournamentParticipant> {
        val standings = standings.associateBy { it.profileId }
        return participants
            .mapNotNull {
                val standing = standings[it.profileId] ?: return@mapNotNull null
                LiveTournamentParticipant(
                    profileId = it.profileId,
                    displayName = it.displayName,
                    image = it.image,
                    registeredAt = it.registeredAt,
                    standing = standing,
                )
            }
            .sortedBy { it.standing.rank }
    }
}

data class LiveTournamentParticipant(
    val profileId: ProfileId,
    val displayName: String,
    val image: String?,
    val registeredAt: Instant,
    val standing: TournamentStanding,
)
