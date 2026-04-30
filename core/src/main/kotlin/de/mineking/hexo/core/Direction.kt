package de.mineking.hexo.core

enum class Direction(val symbol: String, val direction: CellCoordinate) {
    Right("->", CellCoordinate(1, 0)),
    BottomRight("\\>", CellCoordinate(0, 1)),
    BottomLeft("</", CellCoordinate(-1, 1)),
    Left("<-", CellCoordinate(-1, 0)),
    TopLeft("<\\", CellCoordinate(0, -1)),
    TopRight("/>", CellCoordinate(1, -1)),
    ;

    companion object {
        fun fromSymbol(symbol: String) = entries.firstOrNull { it.symbol == symbol }
            ?: throw IllegalArgumentException("Unknown symbol: $symbol. Valid symbols are ${entries.map { it.symbol }}")
    }

    fun ringDirection(sector: Int) = entries[(ordinal + sector + 2) % entries.size].direction
}
