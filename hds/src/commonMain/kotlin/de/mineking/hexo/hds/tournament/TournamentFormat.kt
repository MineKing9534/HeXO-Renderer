package de.mineking.hexo.hds.tournament

sealed interface TournamentFormat {
    companion object {
        internal fun of(dto: TournamentDto, matches: List<TournamentMatch>) = when (dto.format) {
            TournamentFormatType.Swiss -> SwissTournamentFormat(dto, matches)
            TournamentFormatType.SingleElimination -> SingleEliminationTournamentFormat(matches)
            TournamentFormatType.DoubleElimination -> DoubleEliminationTournamentFormat(matches)
        }
    }
}

class SwissTournamentFormat internal constructor(dto: TournamentDto, matches: List<TournamentMatch>) : TournamentFormat {
    val roundCount = dto.swissRoundCount!!
    val rounds: Map<Int, List<TournamentMatch>> = matches
        .groupBy { it.round }
        .mapValues { (_, matches) -> matches.sortedBy { it.order } }
        .toMutableMap()
        .also { map ->
            for (round in 1..roundCount) {
                if (round !in map) {
                    map[round] = emptyList()
                }
            }
        }
}

class SingleEliminationTournamentFormat internal constructor(matches: List<TournamentMatch>) : TournamentFormat {
    // TODO
}

class DoubleEliminationTournamentFormat internal constructor(matches: List<TournamentMatch>) : TournamentFormat {
    // TODO
}
