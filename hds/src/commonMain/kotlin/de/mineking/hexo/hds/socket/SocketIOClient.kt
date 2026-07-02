package de.mineking.hexo.hds.socket

import de.mineking.hexo.hds.HEXO_USER_AGENT
import de.mineking.hexo.hds.utils.withLock
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.Url
import io.ktor.http.protocolWithAuthority
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

internal data class AuthData(val deviceId: String, val ephemeralClientId: String, val versionHash: String = DEFAULT_VERSION_HASH) {
    companion object {
        const val DEFAULT_VERSION_HASH = "HeXO-Kotlin"
    }
}

internal val logger = KotlinLogging.logger {}

data class SocketIOOptions(
    val host: String,
    val path: String,
    val headers: Map<String, String?>,
    val query: Map<String, String>,
) {
    companion object {
        val DEFAULT_HEADERS = mapOf("User-Agent" to HEXO_USER_AGENT)
        val DEFAULT_QUERY_PARAMS = mapOf("skipVersionCheck" to "true")

        fun createDefault(url: String): SocketIOOptions {
            val url = Url(url)
            return SocketIOOptions(
                host = url.protocolWithAuthority,
                path = "${url.encodedPath.trimEnd('/')}/socket.io",
                headers = DEFAULT_HEADERS,
                query = DEFAULT_QUERY_PARAMS,
            )
        }
    }
}

suspend fun connectHexoSocket(json: Json = de.mineking.hexo.hds.json, options: SocketIOOptions): HexoSocketClient {
    logger.info { "Connecting..." }

    val authData = AuthData(
        deviceId = Uuid.random().toString(),
        ephemeralClientId = Uuid.random().toString(),
    )

    val events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val client = SocketIOClient(parent = null) {
        SocketIOClientDriver(json, authData, options)
    }

    SocketEventRegistry.eventNames.keys.forEach { type ->
        client.listen(type) { event ->
            if (!events.tryEmit(event)) {
                logger.warn { "Dropped socket.io event of type '${SocketEventRegistry.eventNames[event::class]}'" }
            }
        }
    }
    client.awaitConnect()

    return HexoSocketClient(client, events)
        .also { logger.info { "Connected" } }
}

class HexoSocketClient(
    val client: SocketIOClient,
    val events: SharedFlow<SocketEvent>,
)

class SocketIOClient internal constructor(
    private val parent: SocketIOClient?,
    private val driverFactory: () -> SocketIOClientDriver,
) {
    private val lock = SynchronizedObject()
    private val children = mutableSetOf<SocketIOClient>()

    private val driver = driverFactory()

    fun fork() = SocketIOClient(this, driverFactory).also {
        lock.withLock {
            children += it
        }
    }

    @IgnorableReturnValue
    fun <T : SocketEvent> listen(type: KClass<T>, handler: EventListener.(T) -> Unit): EventListener {
        lateinit var listener: EventListener
        return driver.listen(type.eventName, type) {
            listener.handler(it)
        }.also {
            listener = it
        }
    }

    fun request(request: SocketRequest) {
        driver.request(request)
    }

    fun connect() = driver.connect()

    @IgnorableReturnValue
    suspend fun awaitConnect() = suspendCancellableCoroutine { continuation ->
        var connectListener: EventListener? = null
        var connectErrorListener: EventListener? = null

        fun removeListeners() {
            connectListener?.remove()
            connectErrorListener?.remove()
            connectListener = null
            connectErrorListener = null
        }

        connectListener = listen<ProtocolSocketEvent.Connected> {
            removeListeners()
            continuation.resume(this@SocketIOClient)
        }

        connectErrorListener = listen<ProtocolSocketEvent.ConnectError> { event ->
            removeListeners()
            continuation.resumeWithException(ConnectErrorException(event.message))
        }

        continuation.invokeOnCancellation {
            removeListeners()
        }

        @Suppress("TooGenericExceptionCaught")
        try {
            connect()
        } catch (e: Exception) {
            removeListeners()
            continuation.resumeWithException(e)
        }
    }

    fun disconnect() {
        parent?.lock?.withLock {
            parent.children -= this
        }

        driver.disconnect()

        val children = lock.withLock {
            children.toList().also { children.clear() }
        }
        children.forEach { it.disconnect() }
    }
}

@IgnorableReturnValue
inline fun <reified T : SocketEvent> SocketIOClient.listen(noinline handler: EventListener.(T) -> Unit) =
    listen(T::class, handler)

interface EventListener {
    fun remove()
}

internal expect class SocketIOClientDriver(
    json: Json,
    authData: AuthData,
    options: SocketIOOptions,
) {
    fun connect()
    fun disconnect()

    @IgnorableReturnValue
    fun <T : SocketEvent> listen(name: String, type: KClass<out T>, handler: (T) -> Unit): EventListener

    fun request(request: SocketRequest)
}

private class ConnectErrorException(override val message: String) : Exception(message)
