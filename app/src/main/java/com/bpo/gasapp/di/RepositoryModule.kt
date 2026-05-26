package com.bpo.gasapp.di

import com.bpo.gasapp.data.repository.AuthRepositoryImpl
import com.bpo.gasapp.data.repository.StationRepositoryImpl
import com.bpo.gasapp.domain.repository.AuthRepository
import com.bpo.gasapp.domain.repository.StationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStationRepository(impl: StationRepositoryImpl): StationRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
