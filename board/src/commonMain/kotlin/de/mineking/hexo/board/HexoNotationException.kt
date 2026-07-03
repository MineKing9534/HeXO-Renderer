package de.mineking.hexo.board

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class HexoNotationException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message)

@OptIn(ExperimentalContracts::class)
inline fun requireHexo(value: Boolean, lazyMessage: () -> Any) {
    contract {
        returns() implies value
    }
    if (!value) {
        val message = lazyMessage()
        throw HexoNotationException(message.toString())
    }
}
