package com.bpo.gasapp.data.repository

import com.bpo.gasapp.data.local.RefuelDao
import com.bpo.gasapp.data.local.VehicleDao
import com.bpo.gasapp.data.local.entity.RefuelEntity
import com.bpo.gasapp.data.remote.RefuelRemoteDataSource
import com.bpo.gasapp.data.remote.RemoteRefuel
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Refuel
import com.bpo.gasapp.domain.repository.RefuelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefuelRepositoryImpl @Inject constructor(
    private val refuelDao: RefuelDao,
    private val vehicleDao: VehicleDao,
    private val remote: RefuelRemoteDataSource
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
                    odometer = entity.odometer,
                    vehicleId = entity.vehicleId,
                    timestamp = entity.timestamp
                )
            }
        }.flowOn(Dispatchers.Default)

    override suspend fun add(
        stationId: String?,
        stationName: String,
        fuel: FuelType,
        liters: Double,
        amount: Double,
        odometer: Double?,
        vehicleId: Long?
    ) {
        val syncId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        refuelDao.insert(
            RefuelEntity(
                stationId = stationId,
                stationName = stationName,
                fuel = fuel.name,
                liters = liters,
                amount = amount,
                odometer = odometer,
                vehicleId = vehicleId,
                timestamp = timestamp,
                syncId = syncId
            )
        )
        remote.put(
            RemoteRefuel(
                syncId = syncId,
                stationId = stationId,
                stationName = stationName,
                fuel = fuel.name,
                liters = liters,
                amount = amount,
                odometer = odometer,
                vehicleSyncId = vehicleId?.let { vehicleDao.getById(it)?.syncId },
                timestamp = timestamp
            )
        )
    }

    override suspend fun delete(id: Long) {
        val syncId = refuelDao.getById(id)?.syncId
        refuelDao.delete(id)
        if (!syncId.isNullOrBlank()) remote.remove(syncId)
    }

    override suspend fun sync() {
        if (!remote.isLoggedIn()) return

        // 1) Asegura un syncId estable para cada repostaje local.
        refuelDao.getAll().forEach { r ->
            if (r.syncId.isBlank()) refuelDao.setSyncId(r.id, UUID.randomUUID().toString())
        }

        val locals = refuelDao.getAll()
        val localBySync = locals.associateBy { it.syncId }
        // Mapas de vehículo: id local <-> syncId, para enlazar entre dispositivos.
        val vehicles = vehicleDao.getAll()
        val vehicleSyncById = vehicles.associate { it.id to it.syncId }
        val vehicleIdBySync = vehicles.filter { it.syncId.isNotBlank() }.associate { it.syncId to it.id }

        val remotes = remote.fetchAll()
        val remoteBySync = remotes.associateBy { it.syncId }

        // 2) Sube los repostajes locales que no existan en la nube.
        locals.filter { it.syncId !in remoteBySync }.forEach { r ->
            remote.put(
                RemoteRefuel(
                    syncId = r.syncId,
                    stationId = r.stationId,
                    stationName = r.stationName,
                    fuel = r.fuel,
                    liters = r.liters,
                    amount = r.amount,
                    odometer = r.odometer,
                    vehicleSyncId = r.vehicleId?.let { vehicleSyncById[it] },
                    timestamp = r.timestamp
                )
            )
        }
        // 3) Descarga los repostajes de la nube que no existan en local.
        remotes.filter { it.syncId !in localBySync }.forEach { r ->
            refuelDao.insert(
                RefuelEntity(
                    stationId = r.stationId,
                    stationName = r.stationName,
                    fuel = r.fuel,
                    liters = r.liters,
                    amount = r.amount,
                    odometer = r.odometer,
                    vehicleId = r.vehicleSyncId?.let { vehicleIdBySync[it] },
                    timestamp = r.timestamp,
                    syncId = r.syncId
                )
            )
        }
    }
}
