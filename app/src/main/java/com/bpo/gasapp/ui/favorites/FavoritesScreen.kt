package com.bpo.gasapp.ui.favorites

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bpo.gasapp.ui.components.StationCard
import com.bpo.gasapp.ui.stations.AD_INTERVAL
import com.bpo.gasapp.ui.stations.CenteredLoader
import com.bpo.gasapp.ui.stations.CenteredMessage
import com.bpo.gasapp.ui.stations.CompactFilterBar
import com.bpo.gasapp.ui.stations.CompactSearchRow
import com.bpo.gasapp.ui.stations.FiltersSheet
import com.bpo.gasapp.ui.stations.LocationBanner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun FavoritesScreen(
    onStationClick: (String) -> Unit,
    onCompareClick: () -> Unit,
    onLogin: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val hasAnyFavorites by viewModel.hasAnyFavorites.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    androidx.compose.runtime.LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) viewModel.refreshLocation()
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.statusBars,
        topBar = { TopAppBar(title = { Text("Gasolineras favoritas") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Mismo encabezado que Inicio: buscador + filtros + actualizar.
            CompactSearchRow(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                onFilters = { showFilters = true },
                onRefresh = viewModel::refresh
            )
            CompactFilterBar(
                fuel = state.filters.fuel,
                sortMode = state.filters.sortMode,
                maxDistanceKm = state.filters.maxDistanceKm,
                zoneAverage = state.zoneAverage,
                onFuelSelect = viewModel::selectFuel,
                onSortSelect = viewModel::setSortMode
            )

            AnimatedVisibility(visible = !state.hasLocation) {
                LocationBanner(onEnable = { locationPermissions.launchMultiplePermissionRequest() })
            }
            AnimatedVisibility(
                visible = !isLoggedIn,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LoginBanner(onLogin = onLogin)
            }

            if (hasAnyFavorites) {
                OutlinedButton(
                    onClick = onCompareClick,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Comparar depósito lleno")
                }
            }

            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isRefreshing && state.stations.isEmpty() -> CenteredLoader()

                    !hasAnyFavorites -> EmptyFavorites()

                    state.stations.isEmpty() ->
                        CenteredMessage("No hay favoritas con estos filtros.")

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.stations.forEachIndexed { index, station ->
                            item(key = station.id) {
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
                            val pos = index + 1
                            if (pos % AD_INTERVAL == 0 && index != state.stations.lastIndex) {
                                item(key = "in-feed-ad-$pos") {
                                    com.bpo.gasapp.ui.ads.BannerAd(
                                        Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        FiltersSheet(
            filters = state.filters,
            availableBrands = state.availableBrands,
            hasLocation = state.hasLocation,
            showOnlyFavoritesToggle = false,
            onChange = viewModel::updateFilters,
            onDismiss = { showFilters = false }
        )
    }
}

@Composable
private fun LoginBanner(onLogin: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Guarda tus favoritas en la nube",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Inicia sesión si quieres guardar tus gasolineras favoritas en tu cuenta y sincronizarlas entre dispositivos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar sesión")
            }
        }
    }
}

@Composable
private fun EmptyFavorites() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(16.dp))
            Text(
                "Aún no tienes gasolineras favoritas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.size(4.dp))
            Text(
                "Pulsa el corazón en cualquier gasolinera para guardarla aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
