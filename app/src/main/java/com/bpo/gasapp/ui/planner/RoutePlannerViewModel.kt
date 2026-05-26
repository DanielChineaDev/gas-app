package com.bpo.gasapp.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.location.AddressGeocoder
import com.bpo.gasapp.data.location.LocationProvider
import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.repository.StationRepository
import com.bpo.gasapp.domain.util.distanceToSegmentMeters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutePlannerUiState(
    val destinationQuery: String = "",
    val corridorKm: Int = 5,
    val selectedFuel: FuelType = FuelType.GASOLINA_95,
    val results: List<Station> = emptyList(),
    val isLoading: Boolean = false,
    val hasPlanned: Boolean = false,
    val error: String? = null
) {
    companion object {
        val CORRIDOR_OPTIONS = listOf(2, 5, 10)
    }
}

@HiltViewModel
class RoutePlannerViewModel @Inject constructor(
    private val repository: StationRepository,
    private val locationProvider: LocationProvider,
    private val geocoder: AddressGeocoder,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutePlannerUiState())
    val uiState: StateFlow<RoutePlannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedFuel = settingsRepository.settings.first().defaultFuel
            )
        }
    }

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(destinationQuery = query)
    }

    fun setCorridor(km: Int) {
        _uiState.value = _uiState.value.copy(corridorKm = km)
        if (_uiState.value.hasPlanned) plan()
    }

    fun selectFuel(fuel: FuelType) {
        _uiState.value = _uiState.value.copy(selectedFuel = fuel)
        if (_uiState.value.hasPlanned) plan()
    }

    fun plan() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            val origin = locationProvider.currentLocation()
            if (origin == null) {
                _uiState.value = state.copy(isLoading = false, error = "Activa la ubicación para planificar la ruta.")
                return@launch
            }
            val destination = geocoder.geocode(state.destinationQuery)
            if (destination == null) {
                _uiState.value = state.copy(isLoading = false, error = "No se encontró el destino. Prueba con otra dirección.")
                return@launch
            }

            val maxMeters = state.corridorKm * 1000f
            val results = repository.observeStations().first()
                .asSequence()
                .filter { it.priceOf(state.selectedFuel) != null }
                .filter {
                    distanceToSegmentMeters(
                        it.latitude, it.longitude,
                        origin.latitude, origin.longitude,
                        destination.latitude, destination.longitude
                    ) <= maxMeters
                }
                .sortedBy { it.priceOf(state.selectedFuel) }
                .take(50)
                .toList()

            _uiState.value = state.copy(
                isLoading = false,
                hasPlanned = true,
                results = results,
                error = if (results.isEmpty()) "No hay gasolineras en el trayecto con ese combustible." else null
            )
        }
    }
}
