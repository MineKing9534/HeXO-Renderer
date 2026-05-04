package de.mineking.hexo.api.socket

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json

data class AuthData(val deviceId: String, val ephemeralClientId: String, val versionHash: String)

internal expect class SocketIOClient(
    json: Json,
    host: String,
    path: String,
    authData: AuthData,
    headers: Map<String, String>,
) {
    val events: SharedFlow<HexoSocketEvent>

    fun disconnect()
}
