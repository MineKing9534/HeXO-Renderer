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
    private val socket = IO.socket(
        host,
        IO.Options.builder()
            .setTransports(arrayOf("websocket"))
            .setExtraHeaders(headers.mapValues { (_, value) -> listOf(value) })
            .setPath(path.let { if (it.endsWith("/")) it else "$it/" })
            .setAuth(
                mapOf(
                    "deviceId" to authData.deviceId,
                    "ephemeralClientId" to authData.ephemeralClientId,
                    "versionHash" to authData.versionHash,
                ),
            )
            .build(),
    ).apply {
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

        on("error") { args ->
            logger.error { "Failed to open socket.io connection: ${args.contentToString()}" }
        }

        connect()
    }

    actual fun disconnect() {
        socket.disconnect()
    }
}
