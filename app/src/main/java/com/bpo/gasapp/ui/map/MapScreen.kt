package com.bpo.gasapp.ui.map

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private val SPAIN_CENTER = LatLng(40.0, -3.7)
private const val MIN_MARKER_ZOOM = 9f

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

    // Recenter on the user the first time we obtain their location.
    LaunchedEffect(state.userLocation) {
        state.userLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 13f)
            )
        }
    }

    // Track the visible bounds and zoom only when the camera settles, to avoid
    // rebuilding the marker list on every frame while panning.
    var visibleBounds by remember { mutableStateOf<LatLngBounds?>(null) }
    var zoom by remember { mutableFloatStateOf(cameraPositionState.position.zoom) }
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }.collect { moving ->
            if (!moving) {
                zoom = cameraPositionState.position.zoom
                visibleBounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
            }
        }
    }

    val clusterItems = rememberVisibleClusterItems(state, visibleBounds, zoom)

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)
        ) {
            Clustering(
                items = clusterItems,
                onClusterItemClick = { item ->
                    onStationClick(item.station.id)
                    true
                },
                clusterContent = { cluster -> ClusterBubble(cluster.size) },
                clusterItemContent = { item -> PriceMarker(item.markerLabel) }
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

        Surface(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            tonalElevation = 3.dp
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FuelType.entries.forEach { fuel ->
                    FilterChip(
                        selected = fuel == state.selectedFuel,
                        onClick = { viewModel.selectFuel(fuel) },
                        label = { Text(fuel.label) }
                    )
                }
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
}

@Composable
private fun PriceMarker(label: String) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .background(
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        androidx.compose.material3.Text(
            text = label,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
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

@Composable
private fun rememberVisibleClusterItems(
    state: MapUiState,
    bounds: LatLngBounds?,
    zoom: Float
): List<StationClusterItem> =
    androidx.compose.runtime.remember(state.stations, state.selectedFuel, bounds, zoom) {
        if (zoom < MIN_MARKER_ZOOM) return@remember emptyList()
        state.stations.asSequence()
            .filter { bounds == null || bounds.contains(LatLng(it.latitude, it.longitude)) }
            .map { station ->
                val price = station.priceOf(state.selectedFuel)
                val label = price?.let { "%.3f".format(it) } ?: "—"
                val snippet = price?.let { "${state.selectedFuel.label}: %.3f €".format(it) }
                StationClusterItem(station, label, snippet)
            }
            .toList()
    }
