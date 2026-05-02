package de.mineking.hexo.api.tournament

sealed interface TournamentOrganization

class SwissTournamentOrganization(tournament: Tournament) : TournamentOrganization {
    val roundCount = tournament.swissRoundCount!!
    val rounds = tournament.matches
        .groupBy { it.round }
        .mapValues { (_, matches) -> matches.sortedBy { it.order } }
}
