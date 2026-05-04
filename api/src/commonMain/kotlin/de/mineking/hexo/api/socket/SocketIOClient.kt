package de.mineking.hexo.api.socket

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json

internal data class AuthData(val deviceId: String, val ephemeralClientId: String, val versionHash: String)

internal expect class SocketIOClient(json: Json, host: String, authData: AuthData) {
    val events: SharedFlow<HexoSocketEvent>

    fun disconnect()
}
