package com.bpo.gasapp.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.location.LocationProvider
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.model.UserLocation
import com.bpo.gasapp.domain.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val stations: List<Station> = emptyList(),
    val selectedFuel: FuelType = FuelType.GASOLINA_95,
    val userLocation: UserLocation? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: StationRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val selectedFuel = MutableStateFlow(FuelType.GASOLINA_95)
    private val userLocation = MutableStateFlow<UserLocation?>(null)

    val uiState: StateFlow<MapUiState> =
        combine(repository.observeStations(), selectedFuel, userLocation) { stations, fuel, location ->
            MapUiState(stations = stations, selectedFuel = fuel, userLocation = location)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState()
        )

    init {
        refreshLocation()
    }

    fun selectFuel(fuel: FuelType) {
        selectedFuel.value = fuel
    }

    fun refreshLocation() {
        viewModelScope.launch { userLocation.value = locationProvider.currentLocation() }
    }
}
