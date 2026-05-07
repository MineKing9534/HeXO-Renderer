package de.mineking.hexo.api

import io.ktor.client.engine.js.Js
import kotlinx.coroutines.Dispatchers

actual val DefaultHttpEngine = Js.create()
actual val DefaultCoroutineDispatcher = Dispatchers.Default
