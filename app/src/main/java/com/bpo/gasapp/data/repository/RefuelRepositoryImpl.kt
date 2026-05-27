package com.bpo.gasapp.data.repository

import com.bpo.gasapp.data.local.RefuelDao
import com.bpo.gasapp.data.local.entity.RefuelEntity
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Refuel
import com.bpo.gasapp.domain.repository.RefuelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefuelRepositoryImpl @Inject constructor(
    private val refuelDao: RefuelDao
) : RefuelRepository {

    override fun observeRefuels(): Flow<List<Refuel>> =
        refuelDao.observeAll().map { list ->
            list.mapNotNull { entity ->
                val fuel = runCatching { FuelType.valueOf(entity.fuel) }.getOrNull() ?: return@mapNotNull null
                Refuel(
                    id = entity.id,
                    stationId = entity.stationId,
                    stationName = entity.stationName,
                    fuel = fuel,
                    liters = entity.liters,
                    amount = entity.amount,
                    timestamp = entity.timestamp
                )
            }
        }.flowOn(Dispatchers.Default)

    override suspend fun add(
        stationId: String?,
        stationName: String,
        fuel: FuelType,
        liters: Double,
        amount: Double
    ) {
        refuelDao.insert(
            RefuelEntity(
                stationId = stationId,
                stationName = stationName,
                fuel = fuel.name,
                liters = liters,
                amount = amount,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun delete(id: Long) = refuelDao.delete(id)
}
