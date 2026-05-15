package de.mineking.hexo.link

import de.mineking.hexo.api.profile.ProfileId
import de.mineking.hexo.link.database.AccountLinkTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert

class AccountLinkRepository(private val database: HexoDatabaseManager) {
    suspend fun getHexoProfile(discordUserId: DiscordUserId) = database.transaction {
        AccountLinkTable
            .select(AccountLinkTable.linkedProfileId)
            .where(AccountLinkTable.id eq discordUserId)
            .firstOrNull()
            ?.get(AccountLinkTable.linkedProfileId)
    }

    @IgnorableReturnValue
    suspend fun removeLinkedProfile(discordUserId: DiscordUserId) = database.transaction {
        AccountLinkTable.deleteWhere { AccountLinkTable.id eq discordUserId } > 0
    }

    @IgnorableReturnValue
    suspend fun createLink(discordUserId: DiscordUserId, linkedProfileId: ProfileId) = database.transaction {
        try {
            AccountLinkTable.upsert {
                it[this.id] = discordUserId
                it[this.linkedProfileId] = linkedProfileId
            }
            true
        } catch (e: ExposedSQLException) {
            if (!e.sqlState.startsWith("23")) throw e
            false
        }
    }
}
