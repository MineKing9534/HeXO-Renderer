package de.mineking.hexo.api

import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.Dispatchers

actual val DefaultHttpEngine = CIO.create()
actual val DefaultCoroutineDispatcher = Dispatchers.IO
