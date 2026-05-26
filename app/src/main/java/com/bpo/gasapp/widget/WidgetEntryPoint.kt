package com.bpo.gasapp.widget

import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.repository.StationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun stationRepository(): StationRepository
    fun settingsRepository(): SettingsRepository
}
