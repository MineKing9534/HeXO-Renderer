package de.mineking.hexo.parse

import de.mineking.hexo.board.Board

interface BoardParser {
    suspend fun parse(notation: String): Board
}
