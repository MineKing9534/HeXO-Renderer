package de.mineking.hexo.hds

import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.Dispatchers

actual val DefaultHttpEngine = CIO.create()
actual val DefaultCoroutineDispatcher = Dispatchers.IO

actual val HEXO_USER_AGENT: String? = "HeXO-Kotlin"
