package de.mineking.hexo.api.profile

import de.mineking.hexo.api.HexoApiClient
import de.mineking.hexo.api.utils.awaitBothOrNull
import io.ktor.client.call.body
import io.ktor.http.isSuccess

interface ProfileRepository {
    suspend fun getProfile(id: ProfileId): Profile?
}

internal class ProfileRepositoryImpl(private val client: HexoApiClient) : ProfileRepository {
    private val requester = client.entityRequesterFactory.createEntityRequester<ProfileId, ProfileDto> {
        val response = client.request("/profiles/${it.value}")

        if (!response.status.isSuccess()) return@createEntityRequester null
        response.body()
    }

    private val statisticsRequester = client.entityRequesterFactory.createEntityRequester<ProfileId, ProfileStatistics> {
        val response = client.request("/profiles/${it.value}/statistics")

        if (!response.status.isSuccess()) return@createEntityRequester null
        response.body()
    }

    override suspend fun getProfile(id: ProfileId): Profile? {
        val (profile, statistics) = awaitBothOrNull(
            first = { requester.fetch(id) },
            second = { statisticsRequester.fetch(id) },
        ) ?: return null

        return Profile.of(profile, statistics)
    }
}
