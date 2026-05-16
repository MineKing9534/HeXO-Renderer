package de.mineking.hexo.link.oauth2

import de.mineking.hexo.link.DiscordUserId
import de.mineking.hexo.link.HexoDatabaseManager
import de.mineking.hexo.link.database.DiscordUserTokensTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.jdbc.deleteReturning
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert

class DiscordUserAuthenticationRepository(
    val database: HexoDatabaseManager,
    val discordOAuth2Client: DiscordOAuth2Client,
) {
    private fun ResultRow.mapToTokens() = OAuth2Tokens(
        client = discordOAuth2Client,
        data = OAuth2TokensDto(
            accessToken = this[DiscordUserTokensTable.accessToken],
            refreshToken = this[DiscordUserTokensTable.refreshToken],
            expiresAt = this[DiscordUserTokensTable.expiresAt],
            scopes = this[DiscordUserTokensTable.scopes],
        ),
        id = this[DiscordUserTokensTable.id].value,
    )

    suspend fun authenticateUser(code: String): OAuth2Tokens? {
        val token = discordOAuth2Client.getUserTokens(code) ?: return null
        require(Scope.Identify in token.data.scopes)

        database.transaction {
            val _ = DiscordUserTokensTable.upsert {
                it[this.id] = token.id
                it[this.accessToken] = token.data.accessToken
                it[this.refreshToken] = token.data.refreshToken
                it[this.expiresAt] = token.data.expiresAt
                it[this.scopes] = token.data.scopes
            }
        }

        return token
    }

    suspend fun getAuthenticationStatus(discordUserId: DiscordUserId) = database.transaction {
        DiscordUserTokensTable
            .select(intLiteral(0))
            .where(DiscordUserTokensTable.id eq discordUserId)
            .firstOrNull() != null
    }

    suspend fun getUserTokens(discordUserId: DiscordUserId): OAuth2Tokens? {
        val tokens = database.transaction {
            DiscordUserTokensTable
                .selectAll()
                .where(DiscordUserTokensTable.id eq discordUserId)
                .firstOrNull()
                ?.mapToTokens()
        }

        if (tokens?.isExpired() != true) return tokens
        val updated = tokens.refresh()

        return database.transaction {
            if (updated == null) {
                DiscordUserTokensTable.deleteWhere { DiscordUserTokensTable.id eq discordUserId }
                null
            } else {
                DiscordUserTokensTable.update(where = { DiscordUserTokensTable.id eq discordUserId }) {
                    it[this.accessToken] = updated.data.accessToken
                    it[this.refreshToken] = updated.data.refreshToken
                    it[this.expiresAt] = updated.data.expiresAt
                }

                updated
            }
        }
    }

    @IgnorableReturnValue
    suspend fun removeUser(userId: DiscordUserId): Boolean {
        val tokens = database.transaction {
            DiscordUserTokensTable.deleteReturning(
                returning = DiscordUserTokensTable.columns,
                where = { DiscordUserTokensTable.id eq userId },
            ).firstOrNull()?.mapToTokens()
        } ?: return false

        tokens.revoke()
        return true
    }
}
