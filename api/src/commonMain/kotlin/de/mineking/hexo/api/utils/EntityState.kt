package de.mineking.hexo.api.utils

sealed interface EntityState<out T> {
    object Loading : EntityState<Nothing>
    object NotFound : EntityState<Nothing>
    class Data<out T>(val value: T) : EntityState<T>
}
