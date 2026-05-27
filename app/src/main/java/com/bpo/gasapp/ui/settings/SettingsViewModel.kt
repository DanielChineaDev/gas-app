package com.bpo.gasapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.remote.ProfileRemoteDataSource
import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.model.AppSettings
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val profileRemote: ProfileRemoteDataSource
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    /** Null while loading from DataStore, then the actual flag. */
    val onboardingDone: StateFlow<Boolean?> = repository.settings
        .map { it.onboardingDone }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun completeOnboarding(fuel: FuelType) {
        viewModelScope.launch {
            repository.setDefaultFuel(fuel)
            repository.setOnboardingDone(true)
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDefaultFuel(fuel: FuelType) {
        viewModelScope.launch {
            repository.setDefaultFuel(fuel)
            profileRemote.setDefaultFuel(fuel)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }
}
