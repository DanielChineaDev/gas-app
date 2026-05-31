package com.bpo.gasapp.ui.map

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.ui.stations.FiltersSheet
import com.bpo.gasapp.ui.theme.FavoriteRed
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlin.math.cos

private val SPAIN_CENTER = LatLng(40.0, -3.7)
private const val MIN_MARKER_ZOOM = 11f
private const val INITIAL_RADIUS_KM = 2.5

@OptIn(
    ExperimentalPermissionsApi::class,
    MapsComposeExperimentalApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun MapScreen(
    onStationClick: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectedStationId by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Pre-carga de logos de marca a bitmap (para dibujarlos en los marcadores
    // de forma síncrona, sin subcomposición). Se cachean por Coil.
    val brandLogos = remember { androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.ui.graphics.ImageBitmap>() }
    val visibleBrands = remember(state.stations) { state.stations.map { it.brand }.distinct() }
    LaunchedEffect(visibleBrands) {
        visibleBrands.forEach { brand ->
            val key = com.bpo.gasapp.ui.components.normalizeBrandKey(brand)
            if (!brandLogos.containsKey(key)) {
                com.bpo.gasapp.ui.components.loadBrandBitmap(context, brand)?.let { brandLogos[key] = it }
            }
        }
    }

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    val hasPermission = locationPermissions.allPermissionsGranted

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.refreshLocation()
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(SPAIN_CENTER, 5.5f)
    }

    // First time we obtain the user's location: recenter and seed an initial
    // 2.5 km region so the map loads only nearby stations right away (before the
    // map projection is even ready).
    LaunchedEffect(state.userLocation) {
        state.userLocation?.let { loc ->
            seedRegion(viewModel, loc.latitude, loc.longitude, INITIAL_RADIUS_KM)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 14f)
            )
        }
    }

    // Once the map is loaded, track the visible region and feed it to the
    // ViewModel only when the camera settles. The ViewModel debounces and caps
    // results, so panning never loads the whole country.
    var mapLoaded by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(cameraPositionState.position.zoom) }
    LaunchedEffect(cameraPositionState, mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        snapshotFlow { cameraPositionState.isMoving }.collect { moving ->
            if (!moving) {
                zoom = cameraPositionState.position.zoom
                val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
                if (zoom >= MIN_MARKER_ZOOM && bounds != null) {
                    viewModel.setVisibleRegion(
                        minLat = bounds.southwest.latitude,
                        maxLat = bounds.northeast.latitude,
                        minLng = bounds.southwest.longitude,
                        maxLng = bounds.northeast.longitude
                    )
                } else if (zoom < MIN_MARKER_ZOOM) {
                    viewModel.clearRegion()
                }
            }
        }
    }

    val clusterItems = rememberClusterItems(state.stations, state.filters.fuel, brandLogos.size)

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false),
            onMapLoaded = { mapLoaded = true }
        ) {
            Clustering(
                items = clusterItems,
                onClusterItemClick = { item ->
                    selectedStationId = item.station.id
                    true
                },
                clusterContent = { cluster -> ClusterBubble(cluster.size) },
                clusterItemContent = { item ->
                    PriceMarker(
                        item.station.brand,
                        item.markerLabel,
                        item.station.isFavorite,
                        brandLogos[com.bpo.gasapp.ui.components.normalizeBrandKey(item.station.brand)]
                    )
                }
            )
        }

        if (zoom < MIN_MARKER_ZOOM) {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                tonalElevation = 3.dp
            ) {
                Text(
                    "Acerca el mapa para ver las gasolineras",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // ── Barra superior: combustible + filtros avanzados + favoritas ──────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            tonalElevation = 3.dp
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FuelDropdownChip(selected = state.filters.fuel, onSelect = viewModel::selectFuel)
                FilterChip(
                    selected = state.filters.onlyFavorites,
                    onClick = { viewModel.updateFilters(state.filters.copy(onlyFavorites = !state.filters.onlyFavorites)) },
                    label = { Text("Favoritas") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (state.filters.onlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (state.filters.onlyFavorites) FavoriteRed else androidx.compose.material3.LocalContentColor.current
                        )
                    }
                )
                FilterChip(
                    selected = state.filters.hasActiveConstraints,
                    onClick = { showFilters = true },
                    label = { Text("Filtros") },
                    leadingIcon = {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = {
                if (!hasPermission) {
                    locationPermissions.launchMultiplePermissionRequest()
                    return@FloatingActionButton
                }
                viewModel.refreshLocation()
                state.userLocation?.let {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 14f)
                        )
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Centrar en mi ubicación")
        }
    }

    val selected = state.stations.firstOrNull { it.id == selectedStationId }
    if (selected != null) {
        StationSheet(
            station = selected,
            fuel = state.filters.fuel,
            onDismiss = { selectedStationId = null },
            onDetail = {
                val id = selected.id
                selectedStationId = null
                onStationClick(id)
            },
            onFavorite = { viewModel.toggleFavorite(selected.id) },
            onNavigate = {
                com.bpo.gasapp.ui.components.openNavigation(context, selected.latitude, selected.longitude)
            }
        )
    }

    if (showFilters) {
        FiltersSheet(
            filters = state.filters,
            availableBrands = emptyList(),
            hasLocation = hasPermission,
            showDistance = false,
            showSort = false,
            onChange = viewModel::updateFilters,
            onDismiss = { showFilters = false }
        )
    }
}

private fun seedRegion(viewModel: MapViewModel, lat: Double, lng: Double, radiusKm: Double) {
    val dLat = radiusKm / 111.32
    val dLng = radiusKm / (111.32 * cos(Math.toRadians(lat)).coerceAtLeast(0.01))
    viewModel.setVisibleRegion(
        minLat = lat - dLat,
        maxLat = lat + dLat,
        minLng = lng - dLng,
        maxLng = lng + dLng
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationSheet(
    station: com.bpo.gasapp.domain.model.Station,
    fuel: FuelType,
    onDismiss: () -> Unit,
    onDetail: () -> Unit,
    onFavorite: () -> Unit,
    onNavigate: () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.bpo.gasapp.ui.components.BrandLogo(brand = station.brand)
                androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    Text(
                        station.brand,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        station.address.ifBlank { station.city },
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
                val shareContext = androidx.compose.ui.platform.LocalContext.current
                androidx.compose.material3.IconButton(onClick = {
                    com.bpo.gasapp.ui.components.shareStation(shareContext, station)
                }) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Share,
                        contentDescription = "Compartir"
                    )
                }
                androidx.compose.material3.IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (station.isFavorite) androidx.compose.material.icons.Icons.Default.Favorite
                        else androidx.compose.material.icons.Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (station.isFavorite) FavoriteRed
                        else androidx.compose.material3.LocalContentColor.current
                    )
                }
            }
            Text(
                text = station.priceOf(fuel)?.let { "${fuel.label}: %.3f €/L".format(it) }
                    ?: "${fuel.label}: no disponible",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onDetail,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                ) { Text("Ver detalle") }
                androidx.compose.material3.Button(
                    onClick = onNavigate,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                ) { Text("Ir") }
            }
        }
    }
}

@Composable
private fun PriceMarker(
    name: String,
    label: String,
    isFavorite: Boolean,
    logo: androidx.compose.ui.graphics.ImageBitmap?
) {
    // Píldora estilo referencia: logo cuadrado a la izquierda; a la derecha,
    // nombre arriba y precio (destacado) debajo.
    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .background(
                color = androidx.compose.ui.graphics.Color.White,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(5.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        // Logo SÍNCRONO (bitmap precargado) para no crashear el renderizador
        // de marcadores; si aún no ha cargado, muestra el avatar de letra.
        com.bpo.gasapp.ui.components.BrandLogoStatic(brand = name, bitmap = logo, size = 34)
        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.size(7.dp))
        androidx.compose.foundation.layout.Column {
            androidx.compose.material3.Text(
                text = name,
                color = androidx.compose.ui.graphics.Color(0xFF2A2F36),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = androidx.compose.ui.Modifier.widthIn(max = 116.dp)
            )
            androidx.compose.material3.Text(
                text = "$label €",
                color = androidx.compose.ui.graphics.Color.Black,
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
        if (isFavorite) {
            androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.size(6.dp))
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Favorita",
                tint = FavoriteRed,
                modifier = androidx.compose.ui.Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun ClusterBubble(count: Int) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .size(40.dp)
            .background(
                color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                shape = androidx.compose.foundation.shape.CircleShape
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = count.toString(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

/**
 * Builds cluster items from the already region-bounded, filtered station list.
 * The list is capped upstream (MAX_MARKERS), so this is cheap.
 */
@Composable
private fun rememberClusterItems(
    stations: List<com.bpo.gasapp.domain.model.Station>,
    fuel: FuelType,
    logoVersion: Int
): List<StationClusterItem> =
    androidx.compose.runtime.remember(stations, fuel, logoVersion) {
        stations.map { station ->
            val price = station.priceOf(fuel)
            val label = price?.let { "%.3f".format(it) } ?: "—"
            val snippet = price?.let { "${fuel.label}: %.3f €".format(it) }
            StationClusterItem(station, label, snippet)
        }
    }
