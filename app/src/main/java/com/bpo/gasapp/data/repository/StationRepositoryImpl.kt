package com.bpo.gasapp.data.repository

import com.bpo.gasapp.data.local.FavoriteDao
import com.bpo.gasapp.data.local.PriceHistoryDao
import com.bpo.gasapp.data.local.StationDao
import com.bpo.gasapp.data.local.entity.FavoriteEntity
import com.bpo.gasapp.data.local.entity.PriceHistoryEntity
import com.bpo.gasapp.data.mapper.toDomain
import com.bpo.gasapp.data.mapper.toEntity
import com.bpo.gasapp.data.remote.FavoritesRemoteDataSource
import com.bpo.gasapp.data.remote.FuelApi
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.PriceDrop
import com.bpo.gasapp.domain.model.PricePoint
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.repository.StationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val api: FuelApi,
    private val stationDao: StationDao,
    private val favoriteDao: FavoriteDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val favoritesRemote: FavoritesRemoteDataSource
) : StationRepository {

    override fun observeStations(): Flow<List<Station>> =
        combine(stationDao.observeAll(), favoriteDao.observeIds()) { stations, favIds ->
            val favSet = favIds.toHashSet()
            stations.map { it.toDomain(isFavorite = it.id in favSet) }
        }.flowOn(Dispatchers.Default)

    override fun observeFavorites(): Flow<List<Station>> =
        combine(stationDao.observeAll(), favoriteDao.observeIds()) { stations, favIds ->
            val favSet = favIds.toHashSet()
            stations.filter { it.id in favSet }.map { it.toDomain(isFavorite = true) }
        }.flowOn(Dispatchers.Default)

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
        val historyEntries = mutableListOf<PriceHistoryEntity>()
        favoriteIds.forEach { id ->
            val updated = stationDao.getById(id)?.toDomain() ?: return@forEach
            updated.prices.forEach { (fuel, newPrice) ->
                historyEntries += PriceHistoryEntity(id, fuel.name, newPrice, now)
                val oldPrice = oldFavorites[id]?.priceOf(fuel)
                if (oldPrice != null && newPrice < oldPrice) {
                    drops += PriceDrop(id, updated.name, fuel, oldPrice, newPrice)
                }
            }
        }
        if (historyEntries.isNotEmpty()) priceHistoryDao.insertAll(historyEntries)
        priceHistoryDao.pruneOlderThan(now - HISTORY_RETENTION_MS)
        drops
    }

    override fun observePriceHistory(stationId: String): Flow<List<PricePoint>> =
        priceHistoryDao.observeForStation(stationId).map { entries ->
            entries.mapNotNull { entry ->
                runCatching { FuelType.valueOf(entry.fuel) }.getOrNull()?.let { fuel ->
                    PricePoint(fuel, entry.price, entry.timestamp)
                }
            }
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

    private companion object {
        const val HISTORY_RETENTION_MS = 90L * 24 * 60 * 60 * 1000
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

    override suspend fun localFavoritesCount(): Int = favoriteDao.count()

    override fun isLoggedIn(): Boolean = favoritesRemote.isLoggedIn()

    override suspend fun resolveFavoritesOnLogin(
        strategy: com.bpo.gasapp.domain.repository.FavoriteMergeStrategy
    ) {
        if (!favoritesRemote.isLoggedIn()) return
        val remoteIds = favoritesRemote.fetchRemoteIds().toHashSet()
        val localIds = favoriteDao.observeIds().first().toHashSet()

        when (strategy) {
            com.bpo.gasapp.domain.repository.FavoriteMergeStrategy.MERGE -> {
                (localIds - remoteIds).forEach { favoritesRemote.add(it) }
                (remoteIds - localIds).forEach { favoriteDao.add(FavoriteEntity(it)) }
            }
            com.bpo.gasapp.domain.repository.FavoriteMergeStrategy.KEEP_LOCAL -> {
                // Local wins: push locals, drop remote-only entries.
                (localIds - remoteIds).forEach { favoritesRemote.add(it) }
                (remoteIds - localIds).forEach { favoritesRemote.remove(it) }
            }
            com.bpo.gasapp.domain.repository.FavoriteMergeStrategy.DISCARD_LOCAL -> {
                // Remote wins: clear local, download remote.
                favoriteDao.clear()
                remoteIds.forEach { favoriteDao.add(FavoriteEntity(it)) }
            }
        }
    }
}
