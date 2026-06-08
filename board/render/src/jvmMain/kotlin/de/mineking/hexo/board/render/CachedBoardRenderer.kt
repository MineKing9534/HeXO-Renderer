package de.mineking.hexo.board.render

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.board.Board

fun <T : Any> BoardRenderer<T>.cached(cacheSize: Long = 16): BoardRenderer<T> = when (this) {
    is CachingBoardRenderer -> this
    else -> CachingBoardRenderer(this, cacheSize)
}

private class CachingBoardRenderer<T : Any>(val delegate: BoardRenderer<T>, cacheSize: Long) : BoardRenderer<T> {
    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<Board, T>()

    override suspend fun Board.render() = cache.get(this) { delegate.run { render() } }
}
