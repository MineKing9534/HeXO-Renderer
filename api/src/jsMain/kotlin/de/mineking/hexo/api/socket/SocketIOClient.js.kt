package de.mineking.hexo.api.socket

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import kotlinx.serialization.serializer
import socketio.AuthPayload
import socketio.SocketOptions
import socketio.invoke
import socketio.io

private val logger = KotlinLogging.logger {}

internal actual class SocketIOClient actual constructor(json: Json, host: String, authData: AuthData) {
    actual val events: SharedFlow<HexoSocketEvent>
        field = MutableSharedFlow<HexoSocketEvent>(extraBufferCapacity = 16)

    private fun AuthData.createOptions() = SocketOptions {
        transports = arrayOf("websocket")
        withCredentials = true
        auth = AuthPayload {
            deviceId = this@createOptions.deviceId
            ephemeralClientId = this@createOptions.ephemeralClientId
            versionHash = this@createOptions.versionHash
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val socket = io(host, authData.createOptions()).apply {
        HexoSocketEvent.eventMappings.forEach { (name, type) ->
            val serializer = json.serializersModule.serializer(type, emptyList(), false)
            on(name) { raw ->
                @Suppress("TooGenericExceptionCaught")
                try {
                    val event = json.decodeFromDynamic(serializer, raw) as HexoSocketEvent
                    if (!events.tryEmit(event)) {
                        logger.warn { "Dropped socket.io event of type '$name'" }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error while handling socket.io event of type '$name'" }
                }
            }
        }

        connect()
    }

    actual fun disconnect() {
        socket.disconnect()
    }
}
