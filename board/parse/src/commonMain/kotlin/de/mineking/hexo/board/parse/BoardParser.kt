package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board

interface BoardParser {
    suspend fun parse(notation: String): Board
}
