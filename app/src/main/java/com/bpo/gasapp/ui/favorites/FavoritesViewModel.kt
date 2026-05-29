package com.bpo.gasapp.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.location.LocationProvider
import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.SortMode
import com.bpo.gasapp.domain.model.StationFilters
import com.bpo.gasapp.domain.model.UserLocation
import com.bpo.gasapp.domain.repository.AuthRepository
import com.bpo.gasapp.domain.repository.StationRepository
import com.bpo.gasapp.domain.util.ScheduleParser
import com.bpo.gasapp.domain.util.distanceMeters
import com.bpo.gasapp.ui.stations.StationListUiState
import com.bpo.gasapp.ui.stations.normalizeForSearch
import com.bpo.gasapp.ui.stations.titleCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: StationRepository,
    private val locationProvider: LocationProvider,
    private val settingsRepository: SettingsRepository,
    authRepository: AuthRepository
) : ViewModel() {

    // Favorites are the user's own picks, so no distance limit by default
    // (a favorite across town should still appear).
    private val filters = MutableStateFlow(StationFilters(maxDistanceKm = null))
    private val searchQuery = MutableStateFlow("")
    private val location = MutableStateFlow<UserLocation?>(null)
    private val isRefreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    /** Whether the user is logged in (favorites are only synced when true). */
    val isLoggedIn: StateFlow<Boolean> = authRepository.authState
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = authRepository.currentUser() != null
        )

    /** Whether the user has any favorite at all (independent of filters/search). */
    val hasAnyFavorites: StateFlow<Boolean> = repository.observeFavorites()
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    init {
        settingsRepository.settings
            .map { it.defaultFuel }
            .distinctUntilChanged()
            .onEach { fuel -> filters.value = filters.value.copy(fuel = fuel) }
            .launchIn(viewModelScope)
        refreshLocation()
    }

    private val filtersAndSearch = combine(filters, searchQuery) { f, q -> f to q }

    val uiState: StateFlow<StationListUiState> =
        combine(
            repository.observeFavorites(),
            filtersAndSearch,
            location,
            isRefreshing,
            error
        ) { favorites, (filters, query), userLocation, refreshing, err ->
            val withDistance = if (userLocation != null) {
                favorites.map {
                    it.copy(distanceMeters = distanceMeters(userLocation, it.latitude, it.longitude))
                }
            } else favorites

            val qNorm = query.trim().normalizeForSearch()
            val filtered = withDistance.filter { station ->
                val brandOk = filters.brands.isEmpty() || station.brand.trim().titleCase() in filters.brands
                val distanceOk = filters.maxDistanceKm == null ||
                    (station.distanceMeters != null &&
                        station.distanceMeters <= filters.maxDistanceKm * 1000f)
                val openOk = !filters.openNowOnly || ScheduleParser.isOpen(station.schedule) != false
                val price = station.priceOf(filters.fuel)
                val priceOk = filters.maxPrice == null || (price != null && price <= filters.maxPrice)
                val searchOk = qNorm.isEmpty() ||
                    station.brand.normalizeForSearch().contains(qNorm) ||
                    station.name.normalizeForSearch().contains(qNorm) ||
                    station.city.normalizeForSearch().contains(qNorm) ||
                    station.province.normalizeForSearch().contains(qNorm)
                brandOk && distanceOk && openOk && priceOk && searchOk
            }

            val prices = filtered.mapNotNull { it.priceOf(filters.fuel) }
            val zoneAverage = if (prices.isNotEmpty()) prices.average() else null

            val sorted = when (filters.sortMode) {
                SortMode.PRICE ->
                    filtered.sortedWith(compareBy(nullsLast()) { it.priceOf(filters.fuel) })
                SortMode.DISTANCE ->
                    filtered.sortedWith(compareBy(nullsLast()) { it.distanceMeters })
                SortMode.VALUE ->
                    filtered.sortedWith(compareBy(nullsLast()) { s ->
                        s.priceOf(filters.fuel)?.plus((s.distanceMeters ?: 0f) / 1000.0 * 0.003)
                    })
                SortMode.SAVING ->
                    filtered.sortedByDescending { s ->
                        val p = s.priceOf(filters.fuel)
                        if (p != null && zoneAverage != null) zoneAverage - p
                        else Double.NEGATIVE_INFINITY
                    }
            }

            // Marcas disponibles entre las favoritas (sin umbral mínimo, son pocas).
            val availableBrands = favorites
                .map { it.brand.trim().titleCase() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            StationListUiState(
                stations = sorted,
                filters = filters,
                searchQuery = query,
                availableBrands = availableBrands,
                zoneAverage = zoneAverage,
                hasLocation = userLocation != null,
                isRefreshing = refreshing,
                error = err
            )
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StationListUiState()
        )

    fun selectFuel(fuel: FuelType) {
        filters.value = filters.value.copy(fuel = fuel)
    }

    fun setSortMode(mode: SortMode) {
        filters.value = filters.value.copy(sortMode = mode)
    }

    fun updateFilters(newFilters: StationFilters) {
        filters.value = newFilters
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun refreshLocation() {
        viewModelScope.launch { location.value = locationProvider.currentLocation() }
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

    fun toggleFavorite(stationId: String) {
        viewModelScope.launch { repository.toggleFavorite(stationId) }
    }
}
