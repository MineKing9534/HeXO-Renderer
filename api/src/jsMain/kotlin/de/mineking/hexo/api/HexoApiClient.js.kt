package de.mineking.hexo.api

import io.ktor.client.engine.js.Js

actual val DefaultHttpEngine = Js.create()
