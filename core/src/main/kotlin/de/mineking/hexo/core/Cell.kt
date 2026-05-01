package de.mineking.hexo.core

enum class Player {
    X {
        override val other get() = O
    },
    O {
        override val other get() = X
    },
    ;

    abstract val other: Player
}

data class Cell(
    var owner: Player? = null,
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

data class HighlightLine(val start: CellCoordinate, val direction: Direction, val length: Int, val color: Player?) {
    init {
        require(length in 1..10)
    }
}

val HighlightLine.end get() = start + direction.direction * length
