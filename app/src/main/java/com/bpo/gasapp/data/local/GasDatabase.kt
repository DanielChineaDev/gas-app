package com.bpo.gasapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bpo.gasapp.data.local.entity.FavoriteEntity
import com.bpo.gasapp.data.local.entity.StationEntity

@Database(
    entities = [StationEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GasDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val NAME = "gasapp.db"
    }
}
