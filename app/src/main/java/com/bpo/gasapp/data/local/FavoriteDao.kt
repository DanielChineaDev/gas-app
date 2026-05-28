package com.bpo.gasapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bpo.gasapp.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT stationId FROM favorites")
    fun observeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE stationId = :stationId")
    suspend fun remove(stationId: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE stationId = :stationId)")
    suspend fun isFavorite(stationId: String): Boolean
}
