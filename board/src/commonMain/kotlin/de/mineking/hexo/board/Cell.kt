package de.mineking.hexo.board

import de.mineking.hexo.core.CellOwner

data class Cell(
    var owner: CellOwner? = null,
    var highlighted: Boolean = false,
    var focussed: Boolean = false,
    var turn: Int? = null,
    var label: String = "",
)

data class CellCoordinate(val q: Int, val r: Int) {
    companion object {
        val Zero = CellCoordinate(0, 0)
    }

    override fun toString() = "($q, $r)"
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
