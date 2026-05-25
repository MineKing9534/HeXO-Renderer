@file:OptIn(ExperimentalSerializationApi::class)

package de.mineking.hexo.api.profile

import de.mineking.hexo.api.utils.Duration
import de.mineking.hexo.api.utils.Instant
import de.mineking.hexo.api.utils.JsonUnwrapSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object ProfileSerializer : JsonUnwrapSerializer<ProfileDto>(ProfileDto.generatedSerializer(), "user")

@KeepGeneratedSerializer
@Serializable(ProfileSerializer::class)
internal data class ProfileDto(
    val id: ProfileId,
    val username: String,
    val image: String?,
    val registeredAt: Instant,
    val lastActiveAt: Instant,
)

internal object ProfileStatisticsSerializer : JsonUnwrapSerializer<ProfileStatistics>(ProfileStatistics.generatedSerializer(), "statistics")

@KeepGeneratedSerializer
@Serializable(ProfileStatisticsSerializer::class)
class ProfileStatistics(
    @SerialName("longestGamePlayedMs") val longestGameByDuration: Duration,
    val longestGameByMoves: Int,
    val totalMovesMade: Int,
    val elo: Int,
    val worldRank: Int?,
    val totalGames: TotalGames,
    val rankedGames: TotalRankedGames,
    val eloHistory: ProfileEloHistory,
) {
    sealed interface GameStatistics {
        val played: Int
        val won: Int

        val winRate get() = won.toDouble() / played
    }

    @Serializable
    data class TotalGames(
        override val played: Int,
        override val won: Int,
    ) : GameStatistics

    @Serializable
    data class TotalRankedGames(
        override val played: Int,
        override val won: Int,
        val currentWinStreak: Int,
        val longestWinStreak: Int,
    ) : GameStatistics
}

@Serializable
data class ProfileEloHistory(
    @SerialName("bucketSizeMs") val bucketSize: Duration,
    val points: List<ProfileEloHistoryPoint>,
) {
    val highestPoint = points.maxByOrNull { it.elo }
}

@Serializable
data class ProfileEloHistoryPoint(
    val timestamp: Instant,
    val elo: Int,
)
