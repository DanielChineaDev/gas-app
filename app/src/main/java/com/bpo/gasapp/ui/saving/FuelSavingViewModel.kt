package com.bpo.gasapp.ui.saving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.location.LocationProvider
import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.model.UserLocation
import com.bpo.gasapp.domain.repository.StationRepository
import com.bpo.gasapp.domain.util.distanceMeters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavingItem(
    val station: Station,
    val price: Double,
    val distanceKm: Float,
    val tankCost: Double,
    /** Net saving vs the nearest station, accounting for detour fuel cost. */
    val netSaving: Double
)

data class FuelSavingUiState(
    val tankLiters: Int = 50,
    val consumption: Double = 6.5,
    val selectedFuel: FuelType = FuelType.GASOLINA_95,
    val items: List<SavingItem> = emptyList(),
    val hasLocation: Boolean = false
)

@HiltViewModel
class FuelSavingViewModel @Inject constructor(
    private val repository: StationRepository,
    private val locationProvider: LocationProvider,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val tankLiters = MutableStateFlow(50)
    private val consumption = MutableStateFlow(6.5)
    private val selectedFuel = MutableStateFlow(FuelType.GASOLINA_95)
    private val location = MutableStateFlow<UserLocation?>(null)

    init {
        viewModelScope.launch { selectedFuel.value = settingsRepository.settings.first().defaultFuel }
        refreshLocation()
    }

    val uiState: StateFlow<FuelSavingUiState> =
        combine(
            repository.observeStations(),
            location,
            selectedFuel,
            tankLiters,
            consumption
        ) { stations, userLocation, fuel, liters, consumptionL100 ->
            if (userLocation == null) {
                return@combine FuelSavingUiState(liters, consumptionL100, fuel, emptyList(), false)
            }

            val nearby = stations
                .mapNotNull { station ->
                    val price = station.priceOf(fuel) ?: return@mapNotNull null
                    val dist = distanceMeters(userLocation, station.latitude, station.longitude)
                    Triple(station, price, dist)
                }
                .filter { it.third <= MAX_RADIUS_METERS }
                .sortedBy { it.third }

            val reference = nearby.firstOrNull()
            val items = if (reference == null) emptyList() else {
                val (_, refPrice, refDist) = reference
                nearby.map { (station, price, dist) ->
                    val priceSaving = (refPrice - price) * liters
                    val extraKm = (dist - refDist).coerceAtLeast(0f) / 1000f * 2f // round trip
                    val detourCost = extraKm / 100.0 * consumptionL100 * price
                    SavingItem(
                        station = station,
                        price = price,
                        distanceKm = dist / 1000f,
                        tankCost = price * liters,
                        netSaving = priceSaving - detourCost
                    )
                }.sortedByDescending { it.netSaving }
            }

            FuelSavingUiState(liters, consumptionL100, fuel, items, true)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FuelSavingUiState()
        )

    fun setLiters(value: Int) { tankLiters.value = value.coerceIn(1, 200) }
    fun setConsumption(value: Double) { consumption.value = value.coerceIn(1.0, 30.0) }
    fun selectFuel(fuel: FuelType) { selectedFuel.value = fuel }
    fun refreshLocation() {
        viewModelScope.launch { location.value = locationProvider.currentLocation() }
    }

    private companion object {
        const val MAX_RADIUS_METERS = 30_000f
    }
}
