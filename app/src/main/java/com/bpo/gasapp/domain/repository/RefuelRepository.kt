package com.bpo.gasapp.domain.repository

import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Refuel
import kotlinx.coroutines.flow.Flow

interface RefuelRepository {
    fun observeRefuels(): Flow<List<Refuel>>
    suspend fun add(stationId: String?, stationName: String, fuel: FuelType, liters: Double, amount: Double, odometer: Double?, vehicleId: Long?)
    suspend fun delete(id: Long)

    /** Sincroniza los repostajes locales con la nube del perfil (fusión en ambos sentidos). */
    suspend fun sync()
}
