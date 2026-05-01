package de.mineking.hexo.api.tournament

import de.mineking.hexo.api.utils.Instant
import de.mineking.hexo.api.utils.TimeControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LiveTournament(data: Tournament) {
    val id = data.id

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

    internal fun update(data: Tournament) {
        require(data.id == id)
        name.value = data.name
        description.value = data.description
        status.value = data.status
        scheduledStart.value = data.scheduledStartAt
        checkInOpensAt.value = data.checkInOpensAt
        checkInClosesAt.value = data.checkInClosesAt
        maxPlayers.value = data.maxPlayers
        registeredCount.value = data.registeredCount
        timeControl.value = data.timeControl
    }
}
