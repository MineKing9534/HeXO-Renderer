package de.mineking.hexo.api

import io.ktor.client.engine.cio.CIO

actual val DefaultHttpEngine = CIO.create()
