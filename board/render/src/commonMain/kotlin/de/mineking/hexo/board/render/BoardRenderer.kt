@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.board.render

import de.mineking.hexo.board.Board

interface BoardRenderer<P, R : Any> {
    suspend fun render(board: Board, param: P): R
}

suspend fun <R : Any> BoardRenderer<Unit, R>.render(board: Board) = render(board, Unit)

fun <P, R : Any> BoardRenderer<P, R>.fixedParam(param: P) = object : BoardRenderer<Unit, R> {
    val param = param
    override suspend fun render(board: Board, param: Unit) = this@fixedParam.render(board, this.param)
}

fun <P> BoardRenderer<P, String>.outputUtf8Bytes() = object : BoardRenderer<P, ByteArray> {
    override suspend fun render(board: Board, param: P) = this@outputUtf8Bytes.render(board, param).encodeToByteArray()
}
