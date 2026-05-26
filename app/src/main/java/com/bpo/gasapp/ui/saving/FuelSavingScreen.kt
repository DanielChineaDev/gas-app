package com.bpo.gasapp.ui.saving

import android.Manifest
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bpo.gasapp.domain.model.FuelType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun FuelSavingScreen(
    onStationClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: FuelSavingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    LaunchedEffect(permissions.allPermissionsGranted) {
        if (permissions.allPermissionsGranted) viewModel.refreshLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modo ahorro") },
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
                "Calcula si compensa ir a una gasolinera más lejana: el ahorro por litro frente al gasto extra del desvío.",
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.tankLiters.toString(),
                    onValueChange = { it.toIntOrNull()?.let(viewModel::setLiters) },
                    label = { Text("Litros") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.consumption.toString(),
                    onValueChange = { it.replace(',', '.').toDoubleOrNull()?.let(viewModel::setConsumption) },
                    label = { Text("L/100km") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
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

            when {
                !state.hasLocation -> Text(
                    "Activa la ubicación para calcular el ahorro.",
                    style = MaterialTheme.typography.bodyMedium
                )
                state.items.isEmpty() -> Text(
                    "No hay gasolineras cercanas con ese combustible.",
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(state.items, key = { _, it -> it.station.id }) { index, item ->
                        SavingCard(item, isBest = index == 0, onClick = { onStationClick(item.station.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingCard(item: SavingItem, isBest: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isBest) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.station.brand, fontWeight = FontWeight.Bold)
                Text(
                    "%.3f € · a %.1f km".format(item.price, item.distanceKm),
                    style = MaterialTheme.typography.bodySmall
                )
                if (isBest) {
                    Text(
                        "Mejor opción",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val sign = if (item.netSaving >= 0) "+" else ""
                Text(
                    "$sign%.2f €".format(item.netSaving),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.netSaving >= 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                Text("ahorro neto", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
