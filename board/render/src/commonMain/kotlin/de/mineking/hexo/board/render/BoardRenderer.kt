@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.board.render

import de.mineking.hexo.board.Board

interface BoardRenderer<T : Any> {
    suspend fun Board.render(): T
}

fun BoardRenderer<String>.outputUtf8Bytes() = object : BoardRenderer<ByteArray> {
    override suspend fun Board.render() = this@outputUtf8Bytes.run { render() }.encodeToByteArray()
}
