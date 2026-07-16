package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.HexoNotationFormatException
import de.mineking.hexo.hds.asBoard
import de.mineking.hexo.hds.formation.FormationId
import de.mineking.hexo.hds.formation.FormationRepository
import de.mineking.hexo.hds.game.FinishedGameRepository
import de.mineking.hexo.hds.game.GameId

abstract class LinkParser(private val prefix: String) : BoardParser {
    final override suspend fun parse(notation: String): Board {
        val trimmedNotation = notation.trim()

        if (!trimmedNotation.startsWith(prefix)) throw HexoNotationFormatException("Invalid link")

        val param = trimmedNotation.removePrefix(prefix)
        if (""".*\s.*""".toRegex().containsMatchIn(param)) throw HexoNotationFormatException("Invalid parameter")

        return parseLink(param)
    }

    abstract suspend fun parseLink(param: String): Board
}

object TytoLinkParser : LinkParser(prefix = "https://hexo.tyto.cc/analysis#c=") {
    override suspend fun parseLink(param: String): Board {
        return param.parseTytoNotation(focusWinningRows = false)
    }
}

class HdsSandboxLinkParser(private val repository: FormationRepository) : LinkParser(prefix = "https://hexo.did.science/sandbox/") {
    override suspend fun parseLink(param: String): Board {
        return repository.getFormation(FormationId(param))
            ?.asBoard(focusWinningRows = false)
            ?: throw HexoNotationException("Formation $param not found")
    }
}

class HdsGameLinkParser(private val repository: FinishedGameRepository) : LinkParser(prefix = "https://hexo.did.science/games/") {
    override suspend fun parseLink(param: String): Board {
        val (id, maxMoves) = param.parseGameLinkParameter()
        return repository.getGame(id)
            ?.asBoard(maxMoves = maxMoves ?: Int.MAX_VALUE, focusWinningRows = false)
            ?: throw HexoNotationException("Game $param not found")
    }
}

private fun String.parseGameLinkParameter(): Pair<GameId, Int?> {
    val parts = split("?move=", limit = 2)
    if (parts.size == 1) return GameId(parts[0]) to null

    val maxMoves = parts[1].toIntOrNull()
        ?: throw HexoNotationException("Invalid move `${parts[1]}`")
    return GameId(parts[0]) to maxMoves
}
