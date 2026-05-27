package com.bpo.gasapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bpo.gasapp.data.local.entity.FavoriteEntity
import com.bpo.gasapp.data.local.entity.PriceHistoryEntity
import com.bpo.gasapp.data.local.entity.RefuelEntity
import com.bpo.gasapp.data.local.entity.StationEntity

@Database(
    entities = [
        StationEntity::class,
        FavoriteEntity::class,
        PriceHistoryEntity::class,
        RefuelEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GasDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun refuelDao(): RefuelDao

    companion object {
        const val NAME = "gasapp.db"
    }
}
