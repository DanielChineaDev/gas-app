package com.bpo.gasapp.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StationListUiState(
    val stations: List<Station> = emptyList(),
    val selectedFuel: FuelType = FuelType.GASOLINA_95,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StationListViewModel @Inject constructor(
    private val repository: StationRepository
) : ViewModel() {

    private val selectedFuel = MutableStateFlow(FuelType.GASOLINA_95)
    private val isRefreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StationListUiState> =
        combine(
            repository.observeStations(),
            selectedFuel,
            isRefreshing,
            error
        ) { stations, fuel, refreshing, err ->
            val sorted = stations.sortedWith(
                compareBy(nullsLast()) { it.priceOf(fuel) }
            )
            StationListUiState(
                stations = sorted,
                selectedFuel = fuel,
                isRefreshing = refreshing,
                error = err
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StationListUiState()
        )

    init {
        // Refresh once on startup if the cache is empty.
        repository.observeStations()
            .onEach { if (it.isEmpty() && !isRefreshing.value) refresh() }
            .launchIn(viewModelScope)
    }

    fun selectFuel(fuel: FuelType) {
        selectedFuel.value = fuel
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
