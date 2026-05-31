package com.bpo.gasapp.domain.repository

import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun observeVehicles(): Flow<List<Vehicle>>
    suspend fun getById(id: Long): Vehicle?
    suspend fun add(name: String, fuel: FuelType, consumption: Double): Long
    suspend fun update(vehicle: Vehicle)
    suspend fun delete(vehicle: Vehicle)

    /** Sincroniza los vehículos locales con la nube del perfil (fusión en ambos sentidos). */
    suspend fun sync()
}
