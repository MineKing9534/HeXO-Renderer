package de.mineking.hexo.parse

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.core.Board

interface BoardParser {
    suspend fun parse(notation: String): Board
}

fun BoardParser.cached(cacheSize: Long = 4): BoardParser = when (this) {
    is CachingBoardParser -> this
    else -> CachingBoardParser(this, cacheSize)
}

private class CachingBoardParser(val delegate: BoardParser, cacheSize: Long) : BoardParser {
    private val cache = Caffeine.newBuilder().maximumSize(cacheSize).asCache<String, Board>()

    override suspend fun parse(notation: String) = cache.get(notation) { delegate.parse(it) }
}
