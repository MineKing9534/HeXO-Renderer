package de.mineking.hexo.bot.utils

import de.mineking.hexo.api.asBoard
import de.mineking.hexo.api.formation.FormationId
import de.mineking.hexo.api.formation.FormationRepository
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.parse.BoardParser

class SandboxFormationOrNotationParser(
    private val formationRepository: FormationRepository,
    private val delegateParser: BoardParser,
) : BoardParser {
    private val positionLinkPattern = """^(#?)https?://hexo\.did\.science/sandbox/([a-zA-Z0-9]{7})$""".toRegex()

    override suspend fun parse(notation: String): Board {
        val match = positionLinkPattern.matchEntire(notation.trim())
        return if (match != null) {
            val showTurnNumbers = match.groupValues[1].isNotEmpty()
            val id = FormationId(match.groupValues[2])
            formationRepository.getFormation(id)?.asBoard(showTurnNumbers)
                ?: throw IllegalArgumentException("Formation with id `${id.value}` not found")
        } else {
            delegateParser.parse(notation)
        }
    }
}
