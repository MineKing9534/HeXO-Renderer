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
)
