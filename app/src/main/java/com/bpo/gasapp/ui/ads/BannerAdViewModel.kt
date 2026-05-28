package com.bpo.gasapp.ui.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BannerAdViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val isPremium: StateFlow<Boolean> = settingsRepository.settings
        .map { it.isPremium }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
