package de.mineking.hexo.core

enum class Player {
    X,
    O,
}

data class Cell(
    var owner: Player? = null,
    var highlighted: Boolean = false,
    var focussed: Boolean = false,
    var turn: Int? = null,
)
