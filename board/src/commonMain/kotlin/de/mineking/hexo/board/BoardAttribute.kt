package de.mineking.hexo.board

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer

@Serializable
sealed class BoardAttribute<T> {
    abstract val default: T
    abstract val serializer: KSerializer<T>

    @Serializable
    data object ShowTurnNumbers : BoardAttribute<Boolean?>() {
        override val default get() = null
        override val serializer = Boolean.serializer().nullable
    }
}

fun BoardAttributes(): BoardAttributes = MutableBoardAttributes()

interface BoardAttributes {
    val values: Map<BoardAttribute<*>, Any?>

    operator fun <T> get(key: BoardAttribute<T>): T

    companion object {
        fun createDefault() = MutableBoardAttributes(mutableMapOf(
            BoardAttribute.ShowTurnNumbers to true,
        ))
    }
}

class MutableBoardAttributes(override val values: MutableMap<BoardAttribute<*>, Any?> = mutableMapOf()) : BoardAttributes {
    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: BoardAttribute<T>) = values.getOrElseIfMissing(key) { key.default } as T

    operator fun <T> set(key: BoardAttribute<T>, value: T) {
        values[key] = value
    }

    override fun hashCode() = values.hashCode()
    override fun equals(other: Any?) = other is MutableBoardAttributes && values == other.values
}

fun BoardAttributes.copy() = MutableBoardAttributes(this@copy.values.toMutableMap())

operator fun BoardAttributes.plus(other: BoardAttributes) = MutableBoardAttributes((this@plus.values + other.values).toMutableMap())
