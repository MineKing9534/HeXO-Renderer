package de.mineking.hexo.core

enum class Player {
    X,
    O,
}

class Cell(
    var owner: Player? = null,
    var marked: Boolean = false,
) {
    fun mark() {
        marked = true
    }

    fun unmark() {
        marked = false
    }
}
