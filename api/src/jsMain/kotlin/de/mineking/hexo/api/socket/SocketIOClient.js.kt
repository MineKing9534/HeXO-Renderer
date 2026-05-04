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
import socketio.toJsObject

private val logger = KotlinLogging.logger {}

internal actual class SocketIOClient actual constructor(
    json: Json,
    host: String,
    path: String,
    authData: AuthData,
    headers: Map<String, String>,
) {
    actual val events: SharedFlow<HexoSocketEvent>
        field = MutableSharedFlow<HexoSocketEvent>(extraBufferCapacity = 16)

    @OptIn(ExperimentalSerializationApi::class)
    private val socket = io(
        url = host,
        options = SocketOptions {
            this.transports = arrayOf("websocket")
            this.extraHeaders = headers.toJsObject()
            this.path = path
            this.addTrailingSlash = true
            this.auth = AuthPayload {
                deviceId = authData.deviceId
                ephemeralClientId = authData.ephemeralClientId
                versionHash = authData.versionHash
            }
        },
    ).apply {
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

        on("error") { args ->
            logger.error { "Failed to open socket.io connection: ${args.contentToString()}" }
        }

        connect()
    }

    actual fun disconnect() {
        socket.disconnect()
    }
}
