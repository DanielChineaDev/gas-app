package com.bpo.gasapp.ui.stations

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import com.bpo.gasapp.domain.model.SortMode
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
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.statusBars
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Cabecera compacta: buscador + botones ────────────────────────
            CompactSearchRow(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                onFilters = { showFilters = true },
                onRefresh = viewModel::refresh
            )

            // ── Fila única de filtros compactos ──────────────────────────────
            CompactFilterBar(
                fuel = state.filters.fuel,
                sortMode = state.filters.sortMode,
                maxDistanceKm = state.filters.maxDistanceKm,
                zoneAverage = state.zoneAverage,
                onFuelSelect = viewModel::selectFuel,
                onSortSelect = viewModel::setSortMode
            )

            androidx.compose.animation.AnimatedVisibility(visible = !state.hasLocation) {
                LocationBanner(onEnable = { locationPermissions.launchMultiplePermissionRequest() })
            }

            // ── Contenido principal ─────────────────────────────────────────
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isRefreshing && state.stations.isEmpty() -> CenteredLoader()

                    state.stations.isEmpty() ->
                        CenteredMessage(state.error ?: "No hay gasolineras con estos filtros.")

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val cheapest = state.stations.minByOrNull {
                            it.priceOf(state.filters.fuel) ?: Double.MAX_VALUE
                        }?.takeIf { it.priceOf(state.filters.fuel) != null }
                        if (cheapest != null) {
                            item(key = "hero") {
                                HeroCard(
                                    station = cheapest,
                                    fuel = state.filters.fuel,
                                    zoneAverage = state.zoneAverage,
                                    onClick = { onStationClick(cheapest.id) }
                                )
                            }
                        }
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

// ─── Cabecera compacta ────────────────────────────────────────────────────────

@Composable
private fun CompactSearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilters: () -> Unit,
    onRefresh: () -> Unit
) {
    var text by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(query) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; onQueryChange(it) },
            modifier = Modifier.weight(1f).height(52.dp),
            placeholder = { Text("Buscar marca, ciudad…", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { text = ""; onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        IconButton(onClick = onFilters) {
            Icon(Icons.Default.FilterList, contentDescription = "Filtros avanzados")
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
        }
    }
}

// ─── Barra de filtros compacta con desplegables ───────────────────────────────

@Composable
private fun CompactFilterBar(
    fuel: FuelType,
    sortMode: SortMode,
    maxDistanceKm: Int?,
    zoneAverage: Double?,
    onFuelSelect: (FuelType) -> Unit,
    onSortSelect: (SortMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FuelDropdownChip(selected = fuel, onSelect = onFuelSelect)
        SortDropdownChip(selected = sortMode, onSelect = onSortSelect)
        DistanceIndicatorChip(maxDistanceKm = maxDistanceKm)

        if (zoneAverage != null) {
            VerticalDivider(modifier = Modifier.height(24.dp))
            Text(
                "Media %.3f €".format(zoneAverage),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FuelDropdownChip(selected: FuelType, onSelect: (FuelType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text(selected.label) },
            trailingIcon = {
                Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FuelType.entries.forEach { fuel ->
                DropdownMenuItem(
                    text = { Text(fuel.label) },
                    onClick = { onSelect(fuel); expanded = false },
                    leadingIcon = if (fuel == selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun SortDropdownChip(selected: SortMode, onSelect: (SortMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            // El modo de ordenación está siempre activo → siempre azul/seleccionado.
            selected = true,
            onClick = { expanded = true },
            label = { Text(selected.label) },
            trailingIcon = {
                Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = { onSelect(mode); expanded = false },
                    leadingIcon = if (mode == selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
    }
}

/**
 * Indicador visual de distancia: muestra el valor activo pero no permite
 * modificarlo directamente. La distancia solo se ajusta desde los filtros
 * avanzados (botón de filtros en la cabecera).
 */
@Composable
private fun DistanceIndicatorChip(maxDistanceKm: Int?) {
    val label = maxDistanceKm?.let { "$it km" } ?: "Sin límite"
    FilterChip(
        selected = maxDistanceKm != null,
        // Solo es indicador visual; no abre ningún selector.
        onClick = {},
        label = { Text(label) },
        leadingIcon = {
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    )
}

// ─── Tarjeta "La más barata" ──────────────────────────────────────────────────

@Composable
private fun HeroCard(
    station: com.bpo.gasapp.domain.model.Station,
    fuel: FuelType,
    zoneAverage: Double?,
    onClick: () -> Unit
) {
    val price = station.priceOf(fuel) ?: return
    val saving = zoneAverage?.let { it - price }
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "⛽ La más barata cerca · ${fuel.label}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                station.brand,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "%.3f €/L".format(price),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                station.distanceMeters?.let {
                    Text(
                        if (it < 1000) "a ${it.toInt()} m" else "a %.1f km".format(it / 1000f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            if (saving != null && saving > 0.001) {
                Text(
                    "Ahorras %.3f €/L respecto a la media de la zona".format(saving),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ─── Aviso de ubicación ───────────────────────────────────────────────────────

@Composable
private fun LocationBanner(onEnable: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = onEnable,
            leadingIcon = { Icon(Icons.Default.LocationOff, contentDescription = null) },
            label = { Text("Activar ubicación para ver distancias") }
        )
    }
}

// ─── Estados de la lista ──────────────────────────────────────────────────────

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
