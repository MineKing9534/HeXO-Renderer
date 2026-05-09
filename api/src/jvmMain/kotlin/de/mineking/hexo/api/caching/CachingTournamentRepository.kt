package de.mineking.hexo.api.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.api.tournament.Tournament
import de.mineking.hexo.api.tournament.TournamentId
import de.mineking.hexo.api.tournament.TournamentRepository
import de.mineking.hexo.api.tournament.isTerminal
import de.mineking.hexo.api.utils.EntityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class CachingTournamentRepository(val delegate: TournamentRepository, cacheSize: Long) : TournamentRepository {
    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<TournamentId, Tournament>()

    override suspend fun getTournament(id: TournamentId): Tournament? {
        cache.getIfPresent(id)?.let { return it }

        return delegate.getTournament(id)?.also {
            if (it.status.isTerminal()) {
                cache.put(id, it)
            }
        }
    }

    override fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>> {
        cache.getOrNull(id)?.let { return MutableStateFlow(EntityState.Data(it)) }
        return delegate.observeTournament(id)
    }
}
