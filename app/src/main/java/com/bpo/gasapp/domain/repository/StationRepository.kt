package com.bpo.gasapp.domain.repository

import com.bpo.gasapp.domain.model.PriceDrop
import com.bpo.gasapp.domain.model.PricePoint
import com.bpo.gasapp.domain.model.Station
import kotlinx.coroutines.flow.Flow

interface StationRepository {

    /** All cached stations, with favorite flag merged in. */
    fun observeStations(): Flow<List<Station>>

    /**
     * Cached stations inside a geographic bounding box, capped to [limit],
     * with favorite flag merged in. Powers the map's region-based loading so
     * it never holds the whole country in memory at once.
     */
    fun observeStationsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        limit: Int
    ): Flow<List<Station>>

    /** Only the user's favorite stations. */
    fun observeFavorites(): Flow<List<Station>>

    suspend fun getStation(id: String): Station?

    /** Recorded price history, oldest first. */
    fun observePriceHistory(stationId: String): Flow<List<PricePoint>>

    /** Saves a history point with the station's current prices (one per hour). */
    suspend fun recordPriceSnapshot(station: Station)

    /** Fetches fresh data from the official API and replaces the local cache. */
    suspend fun refresh(): Result<Unit>

    /**
     * Same as [refresh] but returns the price drops detected on the user's
     * favorite stations (new price < old cached price).
     */
    suspend fun refreshAndDetectFavoriteDrops(): Result<List<PriceDrop>>

    suspend fun toggleFavorite(stationId: String)

    /** Two-way merge of local and remote favorites. No-op if not logged in. */
    suspend fun syncFavorites()

    /** Number of favorites stored locally. */
    suspend fun localFavoritesCount(): Int

    /** Whether there's a logged-in account to sync favorites with. */
    fun isLoggedIn(): Boolean

    /**
     * Resolves local favorites against the remote account on login.
     * - [FavoriteMergeStrategy.MERGE]: union of local + remote.
     * - [FavoriteMergeStrategy.KEEP_LOCAL]: local wins, remote-only entries removed.
     * - [FavoriteMergeStrategy.DISCARD_LOCAL]: remote wins, local entries cleared.
     */
    suspend fun resolveFavoritesOnLogin(strategy: FavoriteMergeStrategy)
}

enum class FavoriteMergeStrategy { MERGE, KEEP_LOCAL, DISCARD_LOCAL }
