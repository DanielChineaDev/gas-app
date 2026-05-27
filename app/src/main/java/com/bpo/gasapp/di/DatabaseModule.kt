package com.bpo.gasapp.di

import android.content.Context
import androidx.room.Room
import com.bpo.gasapp.data.local.FavoriteDao
import com.bpo.gasapp.data.local.GasDatabase
import com.bpo.gasapp.data.local.PriceHistoryDao
import com.bpo.gasapp.data.local.RefuelDao
import com.bpo.gasapp.data.local.StationDao
import com.bpo.gasapp.data.local.VehicleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GasDatabase =
        Room.databaseBuilder(context, GasDatabase::class.java, GasDatabase.NAME)
            .addMigrations(*com.bpo.gasapp.data.local.ALL_MIGRATIONS)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideStationDao(db: GasDatabase): StationDao = db.stationDao()

    @Provides
    fun provideFavoriteDao(db: GasDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun providePriceHistoryDao(db: GasDatabase): PriceHistoryDao = db.priceHistoryDao()

    @Provides
    fun provideRefuelDao(db: GasDatabase): RefuelDao = db.refuelDao()

    @Provides
    fun provideVehicleDao(db: GasDatabase): VehicleDao = db.vehicleDao()
}
