package com.bpo.gasapp.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Vehicle
import com.bpo.gasapp.domain.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehiclesUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedId: Long? = null
)

@HiltViewModel
class VehiclesViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<VehiclesUiState> =
        combine(vehicleRepository.observeVehicles(), settingsRepository.settings) { vehicles, settings ->
            VehiclesUiState(vehicles = vehicles, selectedId = settings.selectedVehicleId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VehiclesUiState()
        )

    fun add(name: String, fuel: FuelType, consumption: Double) {
        viewModelScope.launch {
            val id = vehicleRepository.add(name, fuel, consumption)
            // Auto-select the first vehicle added.
            if (uiState.value.selectedId == null) {
                settingsRepository.setSelectedVehicle(id)
                settingsRepository.setDefaultFuel(fuel)
            }
        }
    }

    fun select(id: Long) {
        viewModelScope.launch {
            settingsRepository.setSelectedVehicle(id)
            vehicleRepository.getById(id)?.let { v ->
                settingsRepository.setDefaultFuel(v.fuel)
            }
        }
    }

    fun delete(vehicle: Vehicle) {
        viewModelScope.launch {
            vehicleRepository.delete(vehicle)
            if (uiState.value.selectedId == vehicle.id) settingsRepository.setSelectedVehicle(null)
        }
    }
}
