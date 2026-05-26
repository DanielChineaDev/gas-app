package com.bpo.gasapp.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<Station> = emptyList(),
    val selectedFuel: FuelType = FuelType.GASOLINA_95
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: StationRepository
) : ViewModel() {

    private val selectedFuel = MutableStateFlow(FuelType.GASOLINA_95)

    val uiState: StateFlow<FavoritesUiState> =
        combine(repository.observeFavorites(), selectedFuel) { favorites, fuel ->
            FavoritesUiState(
                favorites = favorites.sortedWith(compareBy(nullsLast()) { it.priceOf(fuel) }),
                selectedFuel = fuel
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesUiState()
        )

    fun selectFuel(fuel: FuelType) {
        selectedFuel.value = fuel
    }

    fun toggleFavorite(stationId: String) {
        viewModelScope.launch { repository.toggleFavorite(stationId) }
    }
}
