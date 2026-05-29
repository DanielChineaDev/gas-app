package com.bpo.gasapp.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.location.LocationProvider
import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.model.StationFilters
import com.bpo.gasapp.domain.model.UserLocation
import com.bpo.gasapp.domain.repository.StationRepository
import com.bpo.gasapp.domain.util.ScheduleParser
import com.bpo.gasapp.domain.util.distanceMeters
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

data class StationListUiState(
    val stations: List<Station> = emptyList(),
    val filters: StationFilters = StationFilters(),
    val searchQuery: String = "",
    val availableBrands: List<String> = emptyList(),
    val zoneAverage: Double? = null,
    val hasLocation: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StationListViewModel @Inject constructor(
    private val repository: StationRepository,
    private val locationProvider: LocationProvider,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val filters = MutableStateFlow(StationFilters())
    private val searchQuery = MutableStateFlow("")
    private val location = MutableStateFlow<UserLocation?>(null)
    private val isRefreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val dismissedNearbyId = MutableStateFlow<String?>(null)

    /** A station the user is currently at (<100 m), to ask if they refueled. */
    val nearbyStation: StateFlow<Station?> =
        combine(repository.observeStations(), location, dismissedNearbyId) { stations, loc, dismissed ->
            if (loc == null) return@combine null
            stations.asSequence()
                .filter { it.id != dismissed }
                .map { it to distanceMeters(loc, it.latitude, it.longitude) }
                .filter { it.second <= NEARBY_RADIUS_METERS }
                .minByOrNull { it.second }
                ?.first
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val filtersAndSearch = combine(filters, searchQuery) { f, q -> f to q }

    val uiState: StateFlow<StationListUiState> =
        combine(
            repository.observeStations(),
            filtersAndSearch,
            location,
            isRefreshing,
            error
        ) { stations, (filters, query), userLocation, refreshing, err ->
            val withDistance = if (userLocation != null) {
                stations.map {
                    it.copy(distanceMeters = distanceMeters(userLocation, it.latitude, it.longitude))
                }
            } else stations

            val qNorm = query.trim().normalizeForSearch()
            val filtered = withDistance.filter { station ->
                val brandOk = filters.brands.isEmpty() || station.brand.trim().titleCase() in filters.brands
                val distanceOk = filters.maxDistanceKm == null ||
                    (station.distanceMeters != null &&
                        station.distanceMeters <= filters.maxDistanceKm * 1000f)
                val openOk = !filters.openNowOnly || ScheduleParser.isOpen(station.schedule) != false
                val favOk = !filters.onlyFavorites || station.isFavorite
                val price = station.priceOf(filters.fuel)
                val priceOk = filters.maxPrice == null || (price != null && price <= filters.maxPrice)
                val searchOk = qNorm.isEmpty() ||
                    station.brand.normalizeForSearch().contains(qNorm) ||
                    station.name.normalizeForSearch().contains(qNorm) ||
                    station.city.normalizeForSearch().contains(qNorm) ||
                    station.province.normalizeForSearch().contains(qNorm)
                brandOk && distanceOk && openOk && favOk && priceOk && searchOk
            }

            val prices = filtered.mapNotNull { it.priceOf(filters.fuel) }
            val zoneAverage = if (prices.isNotEmpty()) prices.average() else null

            val sorted = when (filters.sortMode) {
                com.bpo.gasapp.domain.model.SortMode.PRICE ->
                    filtered.sortedWith(compareBy(nullsLast()) { it.priceOf(filters.fuel) })
                com.bpo.gasapp.domain.model.SortMode.DISTANCE ->
                    filtered.sortedWith(compareBy(nullsLast()) { it.distanceMeters })
                com.bpo.gasapp.domain.model.SortMode.VALUE ->
                    filtered.sortedWith(compareBy(nullsLast()) { s ->
                        s.priceOf(filters.fuel)?.plus((s.distanceMeters ?: 0f) / 1000.0 * 0.003)
                    })
                // Ahorro respecto a la media de la zona: mayor ahorro primero.
                // Precios desconocidos quedan al final (-infinito).
                com.bpo.gasapp.domain.model.SortMode.SAVING ->
                    filtered.sortedByDescending { s ->
                        val p = s.priceOf(filters.fuel)
                        if (p != null && zoneAverage != null) zoneAverage - p
                        else Double.NEGATIVE_INFINITY
                    }
            }

            // Normaliza marcas (trim + Title Case) y muestra solo las que
            // aparezcan al menos 5 veces para no llenar el filtro de marcas
            // raras / con erratas del dataset oficial.
            val brandCounts = stations
                .map { it.brand.trim().titleCase() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
            val availableBrands = brandCounts
                .filterValues { it >= 5 }
                .entries
                .sortedByDescending { it.value }
                .map { it.key }

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

    init {
        repository.observeStations()
            .onEach { if (it.isEmpty() && !isRefreshing.value) refresh() }
            .launchIn(viewModelScope)
        // Keep the filter fuel in sync with the default fuel. Changing the
        // selected vehicle updates the default fuel, which propagates here.
        settingsRepository.settings
            .map { it.defaultFuel }
            .distinctUntilChanged()
            .onEach { fuel -> filters.value = filters.value.copy(fuel = fuel) }
            .launchIn(viewModelScope)
        refreshLocation()
    }

    fun selectFuel(fuel: FuelType) {
        filters.value = filters.value.copy(fuel = fuel)
    }

    fun updateFilters(newFilters: StationFilters) {
        filters.value = newFilters
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortMode(mode: com.bpo.gasapp.domain.model.SortMode) {
        filters.value = filters.value.copy(sortMode = mode)
    }

    fun refreshLocation() {
        viewModelScope.launch { location.value = locationProvider.currentLocation() }
    }

    fun toggleFavorite(stationId: String) {
        viewModelScope.launch { repository.toggleFavorite(stationId) }
    }

    fun dismissNearby(stationId: String) {
        dismissedNearbyId.value = stationId
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

    private companion object {
        const val NEARBY_RADIUS_METERS = 100f
    }
}

private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")

/** Quita tildes y pasa a minúsculas para que el buscador sea tolerante. */
internal fun String.normalizeForSearch(): String =
    java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()

/** "REPSOL S.A." -> "Repsol S.A." para limpiar las marcas del dataset. */
internal fun String.titleCase(): String =
    lowercase().split(' ').joinToString(" ") { word ->
        word.replaceFirstChar { c -> if (c.isLetter()) c.uppercaseChar() else c }
    }
