package de.mineking.hexo.board

import de.mineking.hexo.core.CellOwner
import kotlin.math.abs

data class Cell(
    var owner: CellOwner? = null,
    var highlight: CellHighlight? = null,
    var focused: Boolean = false,
    var turn: Int? = null,
    var label: String = "",
)

data class CellHighlight(val color: CellOwner?)

data class CellCoordinate(val q: Int, val r: Int) {
    companion object {
        val Zero = CellCoordinate(0, 0)
    }

    override fun toString() = "($q, $r)"
}

fun CellCoordinate.distanceTo(other: CellCoordinate): Int {
    val ds = (q + r) - (other.q + other.r)
    return maxOf(abs(q - other.q), abs(r - other.r), abs(ds))
}

operator fun CellCoordinate.plus(other: CellCoordinate) = CellCoordinate(q + other.q, r + other.r)
operator fun CellCoordinate.minus(other: CellCoordinate) = CellCoordinate(q - other.q, r - other.r)
operator fun CellCoordinate.times(scalar: Int) = CellCoordinate(q * scalar, r * scalar)

data class HighlightLine(val start: CellCoordinate, val direction: Direction, val length: Int, val color: CellOwner?) {
    init {
        requireHexo(length in 1..MAX_LENGTH) { "Length must be between 1 and $MAX_LENGTH" }
    }

    companion object {
        const val MAX_LENGTH = 10
    }
}

val HighlightLine.end get() = start + direction.direction * (length - 1)
operator fun HighlightLine.contains(coordinate: CellCoordinate) = (0 until length).any { coordinate == start + direction.direction * it }
