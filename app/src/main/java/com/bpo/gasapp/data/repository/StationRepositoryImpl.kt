package com.bpo.gasapp.data.repository

import com.bpo.gasapp.data.local.FavoriteDao
import com.bpo.gasapp.data.local.StationDao
import com.bpo.gasapp.data.local.entity.FavoriteEntity
import com.bpo.gasapp.data.mapper.toDomain
import com.bpo.gasapp.data.mapper.toEntity
import com.bpo.gasapp.data.remote.FuelApi
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val api: FuelApi,
    private val stationDao: StationDao,
    private val favoriteDao: FavoriteDao
) : StationRepository {

    override fun observeStations(): Flow<List<Station>> =
        combine(stationDao.observeAll(), favoriteDao.observeIds()) { stations, favIds ->
            val favSet = favIds.toHashSet()
            stations.map { it.toDomain(isFavorite = it.id in favSet) }
        }

    override fun observeFavorites(): Flow<List<Station>> =
        combine(stationDao.observeAll(), favoriteDao.observeIds()) { stations, favIds ->
            val favSet = favIds.toHashSet()
            stations.filter { it.id in favSet }.map { it.toDomain(isFavorite = true) }
        }

    override suspend fun getStation(id: String): Station? {
        val entity = stationDao.getById(id) ?: return null
        return entity.toDomain(isFavorite = favoriteDao.isFavorite(id))
    }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val response = api.getAllStations()
        val now = System.currentTimeMillis()
        val entities = response.estaciones.mapNotNull { it.toEntity(now) }
        if (entities.isNotEmpty()) {
            stationDao.replaceAll(entities)
        }
    }

    override suspend fun toggleFavorite(stationId: String) {
        if (favoriteDao.isFavorite(stationId)) {
            favoriteDao.remove(stationId)
        } else {
            favoriteDao.add(FavoriteEntity(stationId))
        }
    }
}
