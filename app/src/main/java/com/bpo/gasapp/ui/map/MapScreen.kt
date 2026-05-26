package com.bpo.gasapp.ui.map

import android.Manifest
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private val SPAIN_CENTER = LatLng(40.0, -3.7)

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

    val clusterItems = rememberClusterItems(state)

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
                }
            )
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
private fun rememberClusterItems(state: MapUiState): List<StationClusterItem> =
    androidx.compose.runtime.remember(state.stations, state.selectedFuel) {
        state.stations.map { station ->
            val price = station.priceOf(state.selectedFuel)
            val snippet = price?.let { "${state.selectedFuel.label}: %.3f €".format(it) }
            StationClusterItem(station, snippet)
        }
    }
