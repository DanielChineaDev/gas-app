package com.bpo.gasapp.ui.planner

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.ui.components.StationCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlannerScreen(
    onStationClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RoutePlannerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planificar ruta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Desde tu ubicación actual hasta el destino. Te mostramos las gasolineras más baratas a lo largo del trayecto.",
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedTextField(
                value = state.destinationQuery,
                onValueChange = viewModel::setQuery,
                label = { Text("Destino (ciudad, dirección...)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                trailingIcon = {
                    IconButton(onClick = viewModel::plan) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Margen del trayecto", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoutePlannerUiState.CORRIDOR_OPTIONS.forEach { km ->
                    FilterChip(
                        selected = state.corridorKm == km,
                        onClick = { viewModel.setCorridor(km) },
                        label = { Text("$km km") }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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

            Button(onClick = viewModel::plan, modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading) {
                Text("Planificar")
            }

            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
                state.results.isNotEmpty() -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.results, key = { _, s -> s.id }) { index, station ->
                        StationCard(
                            station = station,
                            fuel = state.selectedFuel,
                            isCheapest = index == 0,
                            onClick = { onStationClick(station.id) },
                            onFavorite = {}
                        )
                    }
                }
            }
        }
    }
}
