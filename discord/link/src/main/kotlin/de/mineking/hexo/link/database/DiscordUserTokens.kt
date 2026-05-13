package de.mineking.hexo.link.database

import de.mineking.hexo.link.DiscordUserId
import jdk.jfr.internal.event.EventConfiguration.timestamp
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

internal class DiscordUserTokens(id: EntityID<DiscordUserId>) : Entity<DiscordUserId>(id) {
    companion object : EntityClass<DiscordUserId, DiscordUserTokens>(DiscordUserTokensTable)

    var accessToken by DiscordUserTokensTable.accessToken
    var refreshToken by DiscordUserTokensTable.refreshToken
    var expiresAt by DiscordUserTokensTable.expiresAt
}

internal object DiscordUserTokensTable : IdTable<DiscordUserId>("discord_user_tokens") {
    override val id = long("discord_id").transform({ DiscordUserId(it) }, { it.value }).entityId()

    val accessToken = text("access_token")
    val refreshToken = text("refresh_token")
    val expiresAt = timestamp("expires_at")
}
