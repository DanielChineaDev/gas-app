package com.bpo.gasapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bpo.gasapp.data.local.entity.RefuelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RefuelDao {

    @Insert
    suspend fun insert(refuel: RefuelEntity): Long

    @Query("SELECT * FROM refuels ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<RefuelEntity>>

    @Query("SELECT * FROM refuels")
    suspend fun getAll(): List<RefuelEntity>

    @Query("SELECT * FROM refuels WHERE id = :id")
    suspend fun getById(id: Long): RefuelEntity?

    @Query("UPDATE refuels SET syncId = :syncId WHERE id = :id")
    suspend fun setSyncId(id: Long, syncId: String)

    @Query("DELETE FROM refuels WHERE id = :id")
    suspend fun delete(id: Long)
}
