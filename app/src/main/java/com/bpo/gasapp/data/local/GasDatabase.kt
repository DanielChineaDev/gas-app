package com.bpo.gasapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bpo.gasapp.data.local.entity.FavoriteEntity
import com.bpo.gasapp.data.local.entity.PriceHistoryEntity
import com.bpo.gasapp.data.local.entity.RefuelEntity
import com.bpo.gasapp.data.local.entity.StationEntity
import com.bpo.gasapp.data.local.entity.VehicleEntity

@Database(
    entities = [
        StationEntity::class,
        FavoriteEntity::class,
        PriceHistoryEntity::class,
        RefuelEntity::class,
        VehicleEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class GasDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun refuelDao(): RefuelDao
    abstract fun vehicleDao(): VehicleDao

    companion object {
        const val NAME = "gasapp.db"
    }
}
