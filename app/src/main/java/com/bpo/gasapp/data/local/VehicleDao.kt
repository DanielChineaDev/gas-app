package com.bpo.gasapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bpo.gasapp.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    @Query("SELECT * FROM vehicles ORDER BY name")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles")
    suspend fun getAll(): List<VehicleEntity>

    @Query("UPDATE vehicles SET syncId = :syncId WHERE id = :id")
    suspend fun setSyncId(id: Long, syncId: String)

    @Insert
    suspend fun insert(vehicle: VehicleEntity): Long

    @Update
    suspend fun update(vehicle: VehicleEntity)

    @Delete
    suspend fun delete(vehicle: VehicleEntity)

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: Long): VehicleEntity?
}
