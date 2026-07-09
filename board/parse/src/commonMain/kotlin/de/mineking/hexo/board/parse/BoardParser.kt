package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationFormatException
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.mutable
import de.mineking.hexo.hds.HdsApiClient

interface BoardParser {
    suspend fun parse(notation: String): Board

    companion object {
        internal object None : BoardParser {
            override suspend fun parse(notation: String) = throw NotImplementedError()
        }

        val Default = None
            .or(TytoLinkParser)
            .or(HTTTXNotationParser)
            .or(RectilinearStateBKETurnNotationParser)

        fun createWithHdsSupport(client: HdsApiClient) = None
            .or(HdsGameLinkParser(client.finishedGameRepository))
            .or(HdsSandboxLinkParser(client.formationRepository))
            .or(Default)
    }
}

fun BoardParser.focusWinningRows() = object : BoardParser {
    override suspend fun parse(notation: String) = this@focusWinningRows
        .parse(notation)
        .mutable()
        .focusWinningRows()
}

fun BoardParser.or(other: BoardParser) = when (this) {
    BoardParser.Companion.None -> other
    else -> object : BoardParser {
        override suspend fun parse(notation: String) = try {
            this@or.parse(notation)
        } catch (_: HexoNotationFormatException) {
            other.parse(notation)
        }
    }
}
