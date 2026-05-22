@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.render

import de.mineking.hexo.board.Board

interface BoardRenderer<T : Any> {
    suspend fun Board.render(): T
}
