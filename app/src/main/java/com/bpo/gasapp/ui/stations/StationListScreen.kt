package com.bpo.gasapp.ui.stations

import android.Manifest
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.ui.components.StationCard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StationListScreen(
    onStationClick: (String) -> Unit,
    onLogRefuel: (stationId: String, stationName: String, fuel: String) -> Unit,
    viewModel: StationListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nearby by viewModel.nearbyStation.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) viewModel.refreshLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gasolina barata") },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SearchBar(query = state.searchQuery, onQueryChange = viewModel::setSearchQuery)
            FuelSelector(state.filters.fuel, viewModel::selectFuel)
            SortAndAverageBar(
                sortMode = state.filters.sortMode,
                zoneAverage = state.zoneAverage,
                onSortChange = viewModel::setSortMode
            )

            androidx.compose.animation.AnimatedVisibility(visible = !state.hasLocation) {
                LocationBanner(onEnable = { locationPermissions.launchMultiplePermissionRequest() })
            }

            when {
                state.isRefreshing && state.stations.isEmpty() -> CenteredLoader()

                state.stations.isEmpty() ->
                    CenteredMessage(state.error ?: "No hay gasolineras con estos filtros.")

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = state.stations,
                        key = { _, station -> station.id }
                    ) { index, station ->
                        StationCard(
                            station = station,
                            fuel = state.filters.fuel,
                            isCheapest = index == 0 && station.priceOf(state.filters.fuel) != null,
                            zoneAverage = state.zoneAverage,
                            onClick = { onStationClick(station.id) },
                            onFavorite = { viewModel.toggleFavorite(station.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    nearby?.let { station ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissNearby(station.id) },
            title = { Text("¿Has repostado?") },
            text = { Text("Parece que estás en ${station.brand}. ¿Quieres registrar el repostaje?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.dismissNearby(station.id)
                    onLogRefuel(station.id, station.brand, state.filters.fuel.name)
                }) { Text("Sí, registrar") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissNearby(station.id) }) {
                    Text("Ahora no")
                }
            }
        )
    }

    if (showFilters) {
        FiltersSheet(
            filters = state.filters,
            availableBrands = state.availableBrands,
            hasLocation = state.hasLocation,
            onChange = viewModel::updateFilters,
            onDismiss = { showFilters = false }
        )
    }
}

@Composable
private fun SortAndAverageBar(
    sortMode: com.bpo.gasapp.domain.model.SortMode,
    zoneAverage: Double?,
    onSortChange: (com.bpo.gasapp.domain.model.SortMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        com.bpo.gasapp.domain.model.SortMode.entries.forEach { mode ->
            FilterChip(
                selected = sortMode == mode,
                onClick = { onSortChange(mode) },
                label = { Text(mode.label) }
            )
        }
        if (zoneAverage != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(
                "Media: %.3f €".format(zoneAverage),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        placeholder = { Text("Buscar por marca, ciudad...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar")
                }
            }
        },
        singleLine = true,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    )
}

@Composable
private fun LocationBanner(onEnable: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onEnable,
            leadingIcon = { Icon(Icons.Default.LocationOff, contentDescription = null) },
            label = { Text("Activar ubicación para ver distancias") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelSelector(selected: FuelType, onSelect: (FuelType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FuelType.entries.forEach { fuel ->
            FilterChip(
                selected = fuel == selected,
                onClick = { onSelect(fuel) },
                label = { Text(fuel.label) }
            )
        }
    }
}

@Composable
private fun CenteredLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
