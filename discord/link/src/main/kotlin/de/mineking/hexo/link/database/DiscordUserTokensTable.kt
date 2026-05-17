package de.mineking.hexo.link.database

import de.mineking.hexo.link.DiscordUserId
import de.mineking.hexo.link.oauth2.Scope
import org.jetbrains.exposed.v1.core.EnumerationNameColumnType
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.datetime.timestamp

internal object DiscordUserTokensTable : IdTable<DiscordUserId>("discord_user_tokens") {
    override val id = long("discord_id").transform({ DiscordUserId(it) }, { it.value }).entityId()

    val accessToken = blob("access_token")
    val refreshToken = blob("refresh_token")
    val expiresAt = timestamp("expires_at")
    val scopes = array("scopes", EnumerationNameColumnType(Scope::class, 20))

    override val primaryKey = PrimaryKey(id)
}
