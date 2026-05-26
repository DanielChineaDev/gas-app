package com.bpo.gasapp.di

import android.content.Context
import androidx.room.Room
import com.bpo.gasapp.data.local.FavoriteDao
import com.bpo.gasapp.data.local.GasDatabase
import com.bpo.gasapp.data.local.StationDao
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
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideStationDao(db: GasDatabase): StationDao = db.stationDao()

    @Provides
    fun provideFavoriteDao(db: GasDatabase): FavoriteDao = db.favoriteDao()
}
