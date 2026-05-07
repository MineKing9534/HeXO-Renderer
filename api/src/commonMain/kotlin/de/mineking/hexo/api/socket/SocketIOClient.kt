package de.mineking.hexo.api.socket

import de.mineking.hexo.api.HEXO_USER_AGENT
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.Url
import io.ktor.http.protocolWithAuthority
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

internal data class AuthData(val deviceId: String, val ephemeralClientId: String, val versionHash: String)

internal val logger = KotlinLogging.logger {}

data class SocketIOOptions(
    val host: String,
    val path: String,
    val headers: Map<String, String>,
) {
    companion object {
        fun createDefault(url: String): SocketIOOptions {
            val url = Url(url)
            return SocketIOOptions(
                host = url.protocolWithAuthority,
                path = "${url.encodedPath.trimEnd('/')}/socket.io",
                headers = emptyMap(),
            )
        }
    }
}

class SocketIOClient(private val json: Json, private val options: SocketIOOptions) {
    val events: SharedFlow<SocketEvent>
        field = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 16)

    private fun emitEvent(event: SocketEvent) {
        if (!events.tryEmit(event)) {
            logger.warn { "Dropped socket.io event of type '${SocketEventRegistry.eventNames[event::class]}'" }
        }
    }

    companion object {
        private const val DEFAULT_VERSION_HASH = "version-query"
        private val versionMismatchError =
            "Client version hash ${Regex.escape(DEFAULT_VERSION_HASH)} does not match server version hash (.*). Please refresh the page.".toRegex()

        private val DEFAULT_HEADERS = mapOf("User-Agent" to HEXO_USER_AGENT)
    }

    init {
        val driver = createSocketIODriver(version = DEFAULT_VERSION_HASH)

        driver.listen<ProtocolSocketEvent.ConnectError> { event ->
            val match = versionMismatchError.matchEntire(event.message)
            if (match == null) {
                emitEvent(event)
            } else {
                driver.disconnect()
                val correctVersion = match.groupValues[1]
                logger.info { "Found server version: $correctVersion, reconnecting..." }

                createSocketIODriver(version = correctVersion).connect()
            }
        }

        driver.connect()
    }

    private fun createSocketIODriver(version: String): SocketIOClientDriver {
        val authData = AuthData(
            deviceId = Uuid.random().toString(),
            ephemeralClientId = Uuid.random().toString(),
            versionHash = version,
        )

        val driver = SocketIOClientDriver(json, options.host, options.path, authData, DEFAULT_HEADERS + options.headers)

        lateinit var connectListener: EventListener
        connectListener = driver.listen<ProtocolSocketEvent.Connected> {
            emitEvent(it)
            connectListener.remove()
            SocketEventRegistry.events.forEach { (name, event) ->
                driver.listen(name, event) { event ->
                    emitEvent(event)
                }
            }
        }

        return driver
    }
}

internal interface EventListener {
    fun remove()
}

internal expect class SocketIOClientDriver(
    json: Json,
    host: String,
    path: String,
    authData: AuthData,
    headers: Map<String, String?>,
) {
    fun connect()
    fun disconnect()

    @IgnorableReturnValue
    fun <T : SocketEvent> listen(name: String, type: KClass<out T>, handler: (T) -> Unit): EventListener
}

@IgnorableReturnValue
private inline fun <reified T : SocketEvent> SocketIOClientDriver.listen(noinline handler: (T) -> Unit) =
    listen(SocketEventRegistry.eventNames[T::class]!!, T::class, handler)
