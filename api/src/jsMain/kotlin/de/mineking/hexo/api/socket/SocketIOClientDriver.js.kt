package de.mineking.hexo.api.socket

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import kotlinx.serialization.serializer
import socketio.AuthPayload
import socketio.SocketOptions
import socketio.invoke
import socketio.io
import socketio.toJsObject
import kotlin.reflect.KClass

internal actual class SocketIOClientDriver actual constructor(
    private val json: Json,
    host: String,
    path: String,
    authData: AuthData,
    headers: Map<String, String?>,
) {
    private val socket = io(
        url = host,
        options = SocketOptions {
            this.autoConnect = false
            this.transports = arrayOf("websocket")
            this.extraHeaders = headers.filterValues { it != null }.toJsObject()
            this.path = path
            this.addTrailingSlash = true
            this.auth = AuthPayload {
                deviceId = authData.deviceId
                ephemeralClientId = authData.ephemeralClientId
                versionHash = authData.versionHash
            }
        },
    )

    actual fun connect() {
        socket.connect()
    }

    actual fun disconnect() {
        socket.disconnect()
    }

    @IgnorableReturnValue
    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalSerializationApi::class)
    actual fun <T : SocketEvent> listen(name: String, type: KClass<out T>, handler: (T) -> Unit): EventListener {
        val serializer = json.serializersModule.serializer(type, emptyList(), false)
        val listener = { raw: dynamic ->
            @Suppress("TooGenericExceptionCaught")
            try {
                val event = json.decodeFromDynamic(serializer, raw) as T
                handler(event)
            } catch (e: Exception) {
                logger.error(e) { "Error while handling socket.io event of type '$name'" }
            }
        }
        socket.on(name, listener)
        return object : EventListener {
            override fun remove() {
                socket.off(name, listener)
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    actual fun request(request: SocketRequest) {
        val serializer = json.serializersModule.serializer(request::class, emptyList(), false)
        socket.emit(request.requestName, JSON.parse(json.encodeToString(serializer, request)))
    }
}
