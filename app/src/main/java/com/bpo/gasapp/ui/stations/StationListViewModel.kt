package com.bpo.gasapp.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.location.LocationProvider
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.model.StationFilters
import com.bpo.gasapp.domain.model.UserLocation
import com.bpo.gasapp.domain.repository.StationRepository
import com.bpo.gasapp.domain.util.ScheduleParser
import com.bpo.gasapp.domain.util.distanceMeters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StationListUiState(
    val stations: List<Station> = emptyList(),
    val filters: StationFilters = StationFilters(),
    val availableBrands: List<String> = emptyList(),
    val hasLocation: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StationListViewModel @Inject constructor(
    private val repository: StationRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val filters = MutableStateFlow(StationFilters())
    private val location = MutableStateFlow<UserLocation?>(null)
    private val isRefreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StationListUiState> =
        combine(
            repository.observeStations(),
            filters,
            location,
            isRefreshing,
            error
        ) { stations, filters, userLocation, refreshing, err ->
            val withDistance = if (userLocation != null) {
                stations.map {
                    it.copy(distanceMeters = distanceMeters(userLocation, it.latitude, it.longitude))
                }
            } else stations

            val filtered = withDistance.filter { station ->
                val brandOk = filters.brands.isEmpty() || station.brand in filters.brands
                val distanceOk = filters.maxDistanceKm == null ||
                    (station.distanceMeters != null &&
                        station.distanceMeters <= filters.maxDistanceKm * 1000f)
                val openOk = !filters.openNowOnly || ScheduleParser.isOpen(station.schedule) != false
                brandOk && distanceOk && openOk
            }

            val sorted = filtered.sortedWith(compareBy(nullsLast()) { it.priceOf(filters.fuel) })

            StationListUiState(
                stations = sorted,
                filters = filters,
                availableBrands = stations.map { it.brand }.distinct().sorted(),
                hasLocation = userLocation != null,
                isRefreshing = refreshing,
                error = err
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StationListUiState()
        )

    init {
        repository.observeStations()
            .onEach { if (it.isEmpty() && !isRefreshing.value) refresh() }
            .launchIn(viewModelScope)
        refreshLocation()
    }

    fun selectFuel(fuel: FuelType) {
        filters.value = filters.value.copy(fuel = fuel)
    }

    fun updateFilters(newFilters: StationFilters) {
        filters.value = newFilters
    }

    fun refreshLocation() {
        viewModelScope.launch { location.value = locationProvider.currentLocation() }
    }

    fun toggleFavorite(stationId: String) {
        viewModelScope.launch { repository.toggleFavorite(stationId) }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            error.value = null
            repository.refresh().onFailure {
                error.value = "No se pudieron actualizar los precios. Revisa tu conexión."
            }
            isRefreshing.value = false
        }
    }
}
