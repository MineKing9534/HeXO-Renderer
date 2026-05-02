package de.mineking.hexo.parse

import de.mineking.hexo.core.Board
import de.mineking.hexo.core.CellCoordinate
import de.mineking.hexo.core.Direction
import de.mineking.hexo.core.Player
import de.mineking.hexo.core.plus
import de.mineking.hexo.core.times

enum class Chirality(val symbol: String) {
    Clockwise("CW"),
    CounterClockwise("CCW"),
    ;

    companion object {
        fun fromSymbol(symbol: String) = entries.firstOrNull { it.symbol == symbol }
            ?: throw IllegalArgumentException("Unknown symbol: $symbol. Valid symbols are ${entries.joinToString { "`${it.symbol}`" }}")
    }
}

object BKENotationParser : BoardParser {
    override suspend fun parse(notation: String) = notation.parseBKENotation()
}

fun String.parseBKENotation(
    origin: CellCoordinate? = null,
    zeroOffsetLine: Direction = Direction.Right,
    chirality: Chirality = Chirality.Clockwise,
): Board {
    val turns = parseBKETurns()
    val board = Board()

    val origin = origin ?: run {
        board[0, 0].apply {
            owner = Player.X
            turn = 0
        }
        CellCoordinate.Zero
    }

    turns.forEachIndexed { index, (player, first, second) ->
        fun CellCoordinate.applyMove() = board[this].apply {
            owner = player
            turn = index + 1

            focussed = index == turns.lastIndex
        }

        first.toCellCoordinate(origin, zeroOffsetLine, chirality).applyMove()
        second.toCellCoordinate(origin, zeroOffsetLine, chirality).applyMove()
    }

    return board
}

private data class RingOffset(
    val ring: Int,
    val offset: Int,
)

private data class Turn(
    val player: Player,
    val first: RingOffset,
    val second: RingOffset,
)

private fun String.parseBKETurns(): List<Turn> {
    val parts = trim().split(Regex("\\s+"))
    require(parts.isNotEmpty() && parts.size % 3 == 0) {
        "BKE turns have to be space separated blocks of the format `[x|o] [A-Z]<offset> [A-Z]<offset>`"
    }

    return parts.indices.step(3).map { offset ->
        val (playerChar, first, second) = parts.subList(offset, offset + 3)

        val player = playerChar.parsePlayer()
        val firstMove = first.parseRingOffset()
        val secondMove = second.parseRingOffset()

        Turn(player, firstMove, secondMove)
    }
}

private fun String.parsePlayer() = when (this) {
    "o" -> Player.O
    "x" -> Player.X
    else -> throw IllegalArgumentException("Invalid player `$this`")
}

private fun String.parseRingOffset(): RingOffset {
    require(length >= 2) { "Invalid bke move `$this`, has to be in format `[A-Z]<offset>`" }
    val ring = first().also { require(it in 'A'..'Z') } - 'A' + 1

    val offset = drop(1).let {
        val parts = it.split(".")
        if (parts.size == 1) {
            it.toInt()
        } else {
            require(parts.size == 2) { "Invalid bke move `$this`, has to be in format `[A-Z]<offset>`" }
            val (sector, offset) = parts
            sector.toInt() * ring + offset.toInt()
        }
    }
    require(offset >= 0 && offset < 6 * ring) { "Invalid offset $offset for ring $ring. Offset has to be be in 0..<${6 * ring}" }

    return RingOffset(ring, offset)
}

private fun RingOffset.toCellCoordinate(origin: CellCoordinate, zeroOffsetLine: Direction, chirality: Chirality): CellCoordinate {
    val perimeter = 6 * ring
    val orientedOffset = if (chirality == Chirality.CounterClockwise) (perimeter - offset) % perimeter else offset

    val sector = orientedOffset / ring
    val offset = orientedOffset % ring

    val fullSides = (0 until sector).fold(CellCoordinate.Zero) { acc, i -> acc + zeroOffsetLine.ringDirection(i) * ring }
    return origin + zeroOffsetLine.direction * ring + fullSides + zeroOffsetLine.ringDirection(sector) * offset
}
