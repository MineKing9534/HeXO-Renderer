package de.mineking.hexo.api.formation

import de.mineking.hexo.api.HexoApiClient
import io.ktor.client.call.body
import io.ktor.http.isSuccess

interface FormationRepository {
    suspend fun getFormation(id: FormationId): Formation?
}

internal class FormationRepositoryImpl(private val client: HexoApiClient) : FormationRepository {
    private val requester = client.entityRequesterFactory.createEntityRequester<FormationId, Formation> { id ->
        val response = client.request("/sandbox-positions/${id.value}")

        if (!response.status.isSuccess()) return@createEntityRequester null
        Formation.of(client.host, response.body())
    }

    override suspend fun getFormation(id: FormationId) = requester.fetch(id)
}
