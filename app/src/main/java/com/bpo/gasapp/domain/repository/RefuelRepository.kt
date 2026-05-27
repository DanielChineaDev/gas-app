package com.bpo.gasapp.domain.repository

import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Refuel
import kotlinx.coroutines.flow.Flow

interface RefuelRepository {
    fun observeRefuels(): Flow<List<Refuel>>
    suspend fun add(stationId: String?, stationName: String, fuel: FuelType, liters: Double, amount: Double)
    suspend fun delete(id: Long)
}
