package de.mineking.hexo.bot

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

@Serializable
data class Config(
    val bot: DiscordBotConfig,
    val oauth2: DiscordOAuth2Config? = null,
    val server: ServerConfig? = null,
    val database: DatabaseConfig? = null,
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromEnvironment() = Properties.decodeFromStringMap<Config>(System.getenv())
    }
}

@Serializable
data class DiscordBotConfig(
    val token: String,
)

@Serializable
data class DiscordOAuth2Config(
    val clientId: String,
    val clientSecret: String,
)

@Serializable
data class ServerConfig(
    val port: Int,
    val url: String,
)

@Serializable
data class DatabaseConfig(
    val url: String,
)
