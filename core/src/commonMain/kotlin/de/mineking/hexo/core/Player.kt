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
