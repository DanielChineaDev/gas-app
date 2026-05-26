package com.bpo.gasapp.domain.repository

import com.bpo.gasapp.domain.model.Station
import kotlinx.coroutines.flow.Flow

interface StationRepository {

    /** All cached stations, with favorite flag merged in. */
    fun observeStations(): Flow<List<Station>>

    /** Only the user's favorite stations. */
    fun observeFavorites(): Flow<List<Station>>

    suspend fun getStation(id: String): Station?

    /** Fetches fresh data from the official API and replaces the local cache. */
    suspend fun refresh(): Result<Unit>

    suspend fun toggleFavorite(stationId: String)

    /** Two-way merge of local and remote favorites. No-op if not logged in. */
    suspend fun syncFavorites()
}
