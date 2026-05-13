package de.mineking.hexo.link.database

import de.mineking.hexo.api.profile.ProfileId
import de.mineking.hexo.link.DiscordUserId
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

internal class AccountLink(id: EntityID<DiscordUserId>) : Entity<DiscordUserId>(id) {
    companion object : EntityClass<DiscordUserId, AccountLink>(AccountLinkTable)

    var linkedProfileId by AccountLinkTable.linkedProfileId
}

internal object AccountLinkTable : IdTable<DiscordUserId>("linked_accounts") {
    override val id = long("discord_id").transform({ DiscordUserId(it) }, { it.value }).entityId()
    val linkedProfileId = char("linked_profile_id", 24).transform({ ProfileId(it) }, { it.value })
}
