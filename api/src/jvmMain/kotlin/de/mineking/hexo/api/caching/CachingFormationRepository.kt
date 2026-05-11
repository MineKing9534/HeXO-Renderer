package de.mineking.hexo.api.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.api.formation.Formation
import de.mineking.hexo.api.formation.FormationId
import de.mineking.hexo.api.formation.FormationRepository

internal class CachingFormationRepository(val delegate: FormationRepository, cacheSize: Long) : FormationRepository {
    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<FormationId, Formation>()

    override suspend fun getFormation(id: FormationId) = cache.getOrNull(id) { delegate.getFormation(it) }
}
