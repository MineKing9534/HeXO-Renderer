@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.render

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.clone
import de.mineking.hexo.board.distanceTo
import de.mineking.hexo.board.minus
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.times
import de.mineking.hexo.core.CellOwner

class RectilinearStateBKETurnNotationBoardRenderer(val type: RectilinearNotationType) : BoardRenderer<String> {
    override suspend fun Board.render() = renderRectilinearStateBKETurnNotation(type)
}

fun Board.renderRectilinearStateBKETurnNotation(type: RectilinearNotationType): String {
    val turns = mutableMapOf<Int, Pair<CellOwner, MutableList<CellCoordinate>>>()
    val board = clone()
    cells.forEach { (coordinate, cell) ->
        val owner = cell.owner ?: return@forEach
        val turn = cell.turn ?: return@forEach
        val cells = turns[turn]

        when {
            cells == null -> turns[turn] = owner to mutableListOf(coordinate)
            cells.first == owner -> cells.second += coordinate
            else -> return@forEach
        }

        board.cells[coordinate]?.owner = null
    }

    val state = board.renderRectilinearNotation(type)
    if (turns.isEmpty()) return state

    val renderedTurns = turns.entries
        .filter { it.key > 0 }
        .sortedBy { it.key }
        .map { it.value }
        .renderBKETurns(origin = turns[0]?.second?.firstOrNull(), includePrefix = state.isNotEmpty())

    if (state.isEmpty()) return renderedTurns

    return "$state, $renderedTurns"
}

private fun List<Pair<CellOwner, List<CellCoordinate>>>.renderBKETurns(origin: CellCoordinate?, includePrefix: Boolean) = buildString {
    if (this@renderBKETurns.isEmpty()) {
        append("0")
        return@buildString
    }

    val baseline = Direction.Right
    val origin = origin ?: (this@renderBKETurns.first().second.first() - baseline.direction)

    if (includePrefix) {
        append("${baseline.symbol} @(${origin.q}, ${origin.r}) ")
    }

    this@renderBKETurns.forEach { (owner, cells) ->
        append("${owner.symbol} ")
        cells.forEach { cell ->
            val (ring, sector, sectorOffset) = cell.ringOffset(origin, baseline)
            append('A' + ring - 1)

            if (ring == 1) {
                append(sector)
            } else {
                if (sector != 0) append("$sector.")
                append(sectorOffset)
            }

            append(" ")
        }
    }
}.trimEnd()

private data class RingOffset(val ring: Int, val sector: Int, val sectorOffset: Int)
private fun CellCoordinate.ringOffset(origin: CellCoordinate, baseline: Direction): RingOffset {
    if (this == origin) return RingOffset(0, 0, 0)

    val relative = this - origin
    val ring = distanceTo(origin)

    var corner = baseline.direction * ring
    for (sector in Direction.entries.indices) {
        val direction = baseline.ringDirection(sector)
        for (i in 0 until ring) {
            if (corner + direction * i == relative) {
                return RingOffset(ring = ring, sector = sector, sectorOffset = i)
            }
        }

        corner += direction * ring
    }

    error("Coordinate $relative is not on ring $ring")
}
