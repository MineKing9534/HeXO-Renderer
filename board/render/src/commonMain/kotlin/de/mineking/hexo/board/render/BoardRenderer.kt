@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.board.render

import de.mineking.hexo.board.Board

interface BoardRenderer<T : Any> {
    suspend fun Board.render(): T
}
