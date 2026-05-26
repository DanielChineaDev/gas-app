package com.bpo.gasapp.ui.comparator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TankCost(
    val station: Station,
    val total: Double
)

data class TankComparatorUiState(
    val liters: Int = 50,
    val selectedFuel: FuelType = FuelType.GASOLINA_95,
    val costs: List<TankCost> = emptyList(),
    val savings: Double = 0.0
)

@HiltViewModel
class TankComparatorViewModel @Inject constructor(
    private val repository: StationRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val liters = MutableStateFlow(50)
    private val selectedFuel = MutableStateFlow(FuelType.GASOLINA_95)

    init {
        viewModelScope.launch { selectedFuel.value = settingsRepository.settings.first().defaultFuel }
    }

    val uiState: StateFlow<TankComparatorUiState> =
        combine(repository.observeFavorites(), liters, selectedFuel) { favorites, liters, fuel ->
            val costs = favorites
                .mapNotNull { station -> station.priceOf(fuel)?.let { TankCost(station, it * liters) } }
                .sortedBy { it.total }
            val savings = if (costs.size >= 2) costs.last().total - costs.first().total else 0.0
            TankComparatorUiState(
                liters = liters,
                selectedFuel = fuel,
                costs = costs,
                savings = savings
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TankComparatorUiState()
        )

    fun setLiters(value: Int) {
        liters.value = value.coerceIn(1, 200)
    }

    fun selectFuel(fuel: FuelType) {
        selectedFuel.value = fuel
    }
}
