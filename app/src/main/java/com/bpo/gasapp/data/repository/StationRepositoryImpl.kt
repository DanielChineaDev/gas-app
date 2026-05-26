package com.bpo.gasapp.data.repository

import com.bpo.gasapp.data.local.FavoriteDao
import com.bpo.gasapp.data.local.StationDao
import com.bpo.gasapp.data.local.entity.FavoriteEntity
import com.bpo.gasapp.data.mapper.toDomain
import com.bpo.gasapp.data.mapper.toEntity
import com.bpo.gasapp.data.remote.FavoritesRemoteDataSource
import com.bpo.gasapp.data.remote.FuelApi
import com.bpo.gasapp.domain.model.PriceDrop
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val api: FuelApi,
    private val stationDao: StationDao,
    private val favoriteDao: FavoriteDao,
    private val favoritesRemote: FavoritesRemoteDataSource
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

    override suspend fun refresh(): Result<Unit> =
        refreshAndDetectFavoriteDrops().map { }

    override suspend fun refreshAndDetectFavoriteDrops(): Result<List<PriceDrop>> = runCatching {
        val favoriteIds = favoriteDao.observeIds().first().toSet()
        val oldFavorites = favoriteIds.associateWith { id -> stationDao.getById(id)?.toDomain() }

        val response = api.getAllStations()
        val now = System.currentTimeMillis()
        val entities = response.estaciones.mapNotNull { it.toEntity(now) }
        if (entities.isEmpty()) return@runCatching emptyList()
        stationDao.replaceAll(entities)

        val drops = mutableListOf<PriceDrop>()
        favoriteIds.forEach { id ->
            val old = oldFavorites[id] ?: return@forEach
            val updated = stationDao.getById(id)?.toDomain() ?: return@forEach
            updated.prices.forEach { (fuel, newPrice) ->
                val oldPrice = old.priceOf(fuel)
                if (oldPrice != null && newPrice < oldPrice) {
                    drops += PriceDrop(id, updated.name, fuel, oldPrice, newPrice)
                }
            }
        }
        drops
    }

    override suspend fun toggleFavorite(stationId: String) {
        if (favoriteDao.isFavorite(stationId)) {
            favoriteDao.remove(stationId)
            favoritesRemote.remove(stationId)
        } else {
            favoriteDao.add(FavoriteEntity(stationId))
            favoritesRemote.add(stationId)
        }
    }

    override suspend fun syncFavorites() {
        if (!favoritesRemote.isLoggedIn()) return
        val remoteIds = favoritesRemote.fetchRemoteIds().toHashSet()
        val localIds = favoriteDao.observeIds().first().toHashSet()

        // Upload locals missing in remote.
        (localIds - remoteIds).forEach { favoritesRemote.add(it) }
        // Download remotes missing in local.
        (remoteIds - localIds).forEach { favoriteDao.add(FavoriteEntity(it)) }
    }
}
