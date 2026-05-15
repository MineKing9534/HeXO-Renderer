package de.mineking.hexo.bot.utils

import de.mineking.discord.DiscordToolKit
import de.mineking.discord.localization.LocalizationFile
import de.mineking.hexo.api.createCoroutineScope
import de.mineking.hexo.api.profile.ProfileRepository
import de.mineking.hexo.api.utils.awaitBothOrNull
import de.mineking.hexo.link.AccountLinkRepository
import de.mineking.hexo.link.DiscordUserId
import de.mineking.hexo.link.oauth2.DiscordUserAuthenticationRepository
import de.mineking.hexo.link.oauth2.LinkedRoleMetadataKey
import de.mineking.hexo.link.oauth2.LinkedRoleMetadataType
import de.mineking.hexo.link.oauth2.bindValue
import de.mineking.hexo.link.oauth2.updateLinkedRoleMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val RankKey = LinkedRoleMetadataKey("rank", LinkedRoleMetadataType.IntegerLessThanOrEqual)
private val EloKey = LinkedRoleMetadataKey("elo", LinkedRoleMetadataType.IntegerGreaterThanOrEqual)

private val logger = KotlinLogging.logger {}

class LinkedRolesUpdateService(
    private val accountLinkRepository: AccountLinkRepository,
    private val discordUserAuthenticationRepository: DiscordUserAuthenticationRepository,
    private val profileRepository: ProfileRepository,
) {
    private val coroutineScope = createCoroutineScope(logger)
    private val semaphore = Semaphore(10)

    fun scheduleLinkedRoleDataUpdate(user: DiscordUserId) {
        coroutineScope.launch {
            semaphore.withPermit {
                updateLinkedRoleData(user)
            }
        }
    }

    private suspend fun updateLinkedRoleData(user: DiscordUserId) {
        val (profile, tokens) = awaitBothOrNull(
            first = {
                val linkedProfileId = accountLinkRepository.getHexoProfile(user) ?: return@awaitBothOrNull null
                profileRepository.getProfile(linkedProfileId)
            },
            second = { discordUserAuthenticationRepository.getUserTokens(user) },
        ) ?: return

        discordUserAuthenticationRepository.discordOAuth2Client.updateLinkedRoleMetadata(
            user = tokens,
            values = arrayOf(
                RankKey.bindValue(profile.statistics.worldRank),
                EloKey.bindValue(profile.statistics.elo),
            ),
        )
    }
}

suspend fun DiscordToolKit<*>.updateLinkedRoleMetadata() = updateLinkedRoleMetadata<LinkedRolesMetadataLocalization>(
    RankKey,
    EloKey,
)

interface LinkedRolesMetadataLocalization : LocalizationFile
