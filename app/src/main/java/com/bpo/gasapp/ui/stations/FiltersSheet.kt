package com.bpo.gasapp.ui.stations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bpo.gasapp.domain.model.StationFilters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltersSheet(
    filters: StationFilters,
    availableBrands: List<String>,
    hasLocation: Boolean,
    onChange: (StationFilters) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Filtros", fontWeight = FontWeight.Bold)
                TextButton(onClick = { onChange(StationFilters(fuel = filters.fuel)) }) {
                    Text("Limpiar")
                }
            }

            Text("Distancia", fontWeight = FontWeight.SemiBold)
            if (!hasLocation) {
                Text(
                    "Activa la ubicación para filtrar por distancia.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StationFilters.DISTANCE_OPTIONS.forEach { km ->
                    FilterChip(
                        selected = filters.maxDistanceKm == km,
                        onClick = {
                            onChange(filters.copy(maxDistanceKm = if (filters.maxDistanceKm == km) null else km))
                        },
                        enabled = hasLocation,
                        label = { Text("$km km") }
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Solo abiertas ahora", fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = filters.openNowOnly,
                    onCheckedChange = { onChange(filters.copy(openNowOnly = it)) }
                )
            }

            if (availableBrands.isNotEmpty()) {
                Text("Marca", fontWeight = FontWeight.SemiBold)
                FlowRow(
                    modifier = Modifier.heightIn(max = 240.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableBrands.forEach { brand ->
                        val selected = brand in filters.brands
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val updated = filters.brands.toMutableSet().apply {
                                    if (selected) remove(brand) else add(brand)
                                }
                                onChange(filters.copy(brands = updated))
                            },
                            label = { Text(brand) }
                        )
                    }
                }
            }
        }
    }
}
