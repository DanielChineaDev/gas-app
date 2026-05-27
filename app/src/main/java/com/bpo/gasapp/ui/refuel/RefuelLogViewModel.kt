package com.bpo.gasapp.ui.refuel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.repository.RefuelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RefuelLogUiState(
    val stationName: String = "",
    val fuel: FuelType = FuelType.GASOLINA_95,
    val liters: String = "",
    val amount: String = "",
    val isProcessingPhoto: Boolean = false,
    val photoMessage: String? = null,
    val saved: Boolean = false
) {
    val pricePerLiter: Double?
        get() {
            val l = liters.replace(',', '.').toDoubleOrNull()
            val a = amount.replace(',', '.').toDoubleOrNull()
            return if (l != null && a != null && l > 0) a / l else null
        }
    val canSave: Boolean
        get() = liters.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true &&
            amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true
}

@HiltViewModel
class RefuelLogViewModel @Inject constructor(
    private val refuelRepository: RefuelRepository,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val stationId: String? =
        savedStateHandle.get<String>(RefuelLogRoute.ARG_STATION_ID)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(RefuelLogUiState())
    val uiState: StateFlow<RefuelLogUiState> = _uiState.asStateFlow()

    init {
        val name = savedStateHandle.get<String>(RefuelLogRoute.ARG_STATION_NAME).orEmpty()
        val fuelArg = savedStateHandle.get<String>(RefuelLogRoute.ARG_FUEL)
            ?.let { runCatching { FuelType.valueOf(it) }.getOrNull() }
        _uiState.value = _uiState.value.copy(stationName = name)
        viewModelScope.launch {
            val fuel = fuelArg ?: settingsRepository.settings.first().defaultFuel
            _uiState.value = _uiState.value.copy(fuel = fuel)
        }
    }

    fun setStationName(v: String) { _uiState.value = _uiState.value.copy(stationName = v) }
    fun setFuel(v: FuelType) { _uiState.value = _uiState.value.copy(fuel = v) }
    fun setLiters(v: String) { _uiState.value = _uiState.value.copy(liters = v) }
    fun setAmount(v: String) { _uiState.value = _uiState.value.copy(amount = v) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            refuelRepository.add(
                stationId = stationId,
                stationName = state.stationName.ifBlank { "Repostaje" },
                fuel = state.fuel,
                liters = state.liters.replace(',', '.').toDouble(),
                amount = state.amount.replace(',', '.').toDouble()
            )
            _uiState.value = state.copy(saved = true)
        }
    }
}
