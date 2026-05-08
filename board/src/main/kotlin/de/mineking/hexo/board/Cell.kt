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
}

operator fun CellCoordinate.plus(other: CellCoordinate) = CellCoordinate(q + other.q, r + other.r)
operator fun CellCoordinate.minus(other: CellCoordinate) = CellCoordinate(q - other.q, r - other.r)
operator fun CellCoordinate.times(scalar: Int) = CellCoordinate(q * scalar, r * scalar)

data class HighlightLine(val start: CellCoordinate, val direction: Direction, val length: Int, val color: CellOwner?) {
    init {
        require(length in 1..10)
    }
}

val HighlightLine.end get() = start + direction.direction * (length - 1)
