package de.mineking.hexo.link

import de.mineking.hexo.link.database.AccountLinkTable
import de.mineking.hexo.link.database.DiscordUserTokensTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

class HexoDatabaseManager(url: String) {
    private val database = Database.connect(url)

    init {
        transaction(database) {
            MigrationUtils.statementsRequiredForDatabaseMigration(DiscordUserTokensTable, AccountLinkTable).forEach {
                exec(it)
            }
        }
    }

    internal suspend fun <T> transaction(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        suspendTransaction(database) {
            block()
        }
    }
}
