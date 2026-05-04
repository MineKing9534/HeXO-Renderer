package de.mineking.hexo.api.socket

import io.github.oshai.kotlinlogging.KotlinLogging
import io.socket.client.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.json.JSONObject

private val logger = KotlinLogging.logger {}

internal actual class SocketIOClient actual constructor(json: Json, host: String, authData: AuthData) {
    actual val events: SharedFlow<HexoSocketEvent>
        field = MutableSharedFlow<HexoSocketEvent>(extraBufferCapacity = 16)

    private fun AuthData.createOptions() = IO.Options.builder()
        .setAuth(
            mapOf(
                "deviceId" to deviceId,
                "ephemeralClientId" to ephemeralClientId,
                "versionHash" to versionHash,
            ),
        )
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    private val socket = IO.socket(host, authData.createOptions()).apply {
        HexoSocketEvent.eventMappings.forEach { (name, type) ->
            val serializer = json.serializersModule.serializer(type, emptyList(), false)
            on(name) { args ->
                @Suppress("TooGenericExceptionCaught")
                try {
                    val string = when (val raw = args[0]) {
                        is String -> raw
                        is JSONObject -> raw.toString()
                        else -> error("Unsupported event type: ${raw.javaClass}")
                    }

                    val event = json.decodeFromString(serializer, string) as HexoSocketEvent
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
