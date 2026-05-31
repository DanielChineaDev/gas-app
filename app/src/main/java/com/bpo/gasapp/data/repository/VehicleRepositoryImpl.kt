package com.bpo.gasapp.data.repository

import com.bpo.gasapp.data.local.VehicleDao
import com.bpo.gasapp.data.local.entity.VehicleEntity
import com.bpo.gasapp.data.remote.RemoteVehicle
import com.bpo.gasapp.data.remote.VehicleRemoteDataSource
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Vehicle
import com.bpo.gasapp.domain.repository.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepositoryImpl @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val remote: VehicleRemoteDataSource
) : VehicleRepository {

    override fun observeVehicles(): Flow<List<Vehicle>> =
        vehicleDao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override suspend fun getById(id: Long): Vehicle? = vehicleDao.getById(id)?.toDomain()

    override suspend fun add(name: String, fuel: FuelType, consumption: Double): Long {
        val syncId = UUID.randomUUID().toString()
        val id = vehicleDao.insert(
            VehicleEntity(name = name, fuel = fuel.name, consumption = consumption, syncId = syncId)
        )
        remote.put(RemoteVehicle(syncId, name, fuel.name, consumption))
        return id
    }

    override suspend fun update(vehicle: Vehicle) {
        // Conserva el syncId existente (el modelo de dominio no lo expone).
        val syncId = vehicleDao.getById(vehicle.id)?.syncId?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        vehicleDao.update(
            VehicleEntity(vehicle.id, vehicle.name, vehicle.fuel.name, vehicle.consumption, syncId)
        )
        remote.put(RemoteVehicle(syncId, vehicle.name, vehicle.fuel.name, vehicle.consumption))
    }

    override suspend fun delete(vehicle: Vehicle) {
        val syncId = vehicleDao.getById(vehicle.id)?.syncId
        vehicleDao.delete(VehicleEntity(vehicle.id, vehicle.name, vehicle.fuel.name, vehicle.consumption, syncId ?: ""))
        if (!syncId.isNullOrBlank()) remote.remove(syncId)
    }

    override suspend fun sync() {
        if (!remote.isLoggedIn()) return
        val locals = vehicleDao.getAll()

        // 1) Asegura un syncId estable para cada vehículo local.
        locals.forEach { v ->
            if (v.syncId.isBlank()) vehicleDao.setSyncId(v.id, UUID.randomUUID().toString())
        }
        val localsWithIds = vehicleDao.getAll()
        val localBySync = localsWithIds.associateBy { it.syncId }

        val remotes = remote.fetchAll()
        val remoteBySync = remotes.associateBy { it.syncId }

        // 2) Sube los vehículos locales que no existan en la nube.
        localsWithIds.filter { it.syncId !in remoteBySync }.forEach { v ->
            remote.put(RemoteVehicle(v.syncId, v.name, v.fuel, v.consumption))
        }
        // 3) Descarga los vehículos de la nube que no existan en local.
        remotes.filter { it.syncId !in localBySync }.forEach { r ->
            vehicleDao.insert(
                VehicleEntity(name = r.name, fuel = r.fuel, consumption = r.consumption, syncId = r.syncId)
            )
        }
    }

    private fun VehicleEntity.toDomain(): Vehicle = Vehicle(
        id = id,
        name = name,
        fuel = runCatching { FuelType.valueOf(fuel) }.getOrDefault(FuelType.GASOLINA_95),
        consumption = consumption
    )
}
