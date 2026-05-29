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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bpo.gasapp.domain.model.SortMode
import com.bpo.gasapp.domain.model.StationFilters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltersSheet(
    filters: StationFilters,
    availableBrands: List<String>,
    hasLocation: Boolean,
    onChange: (StationFilters) -> Unit,
    onDismiss: () -> Unit,
    showDistance: Boolean = true,
    showSort: Boolean = true,
    showOnlyFavoritesToggle: Boolean = true
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
                Text("Filtros avanzados", fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    onChange(
                        StationFilters(
                            fuel = filters.fuel,
                            maxDistanceKm = if (showDistance) StationFilters().maxDistanceKm else null
                        )
                    )
                }) {
                    Text("Limpiar")
                }
            }

            // ── Ordenar por (no aplica en el mapa) ───────────────────────────
            if (showSort) {
                Text("Ordenar por", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = filters.sortMode == mode,
                            onClick = { onChange(filters.copy(sortMode = mode)) },
                            label = { Text(mode.label) }
                        )
                    }
                }
                HorizontalDivider()
            }

            // ── Precio máximo ────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Precio máximo", fontWeight = FontWeight.SemiBold)
                Text(
                    filters.maxPrice?.let { "%.2f €/L".format(it) } ?: "Sin límite",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = (filters.maxPrice ?: StationFilters.PRICE_MAX).toFloat(),
                onValueChange = {
                    onChange(filters.copy(maxPrice = (Math.round(it * 100.0) / 100.0)))
                },
                valueRange = StationFilters.PRICE_MIN.toFloat()..StationFilters.PRICE_MAX.toFloat(),
                steps = 29
            )
            TextButton(
                onClick = { onChange(filters.copy(maxPrice = null)) },
                enabled = filters.maxPrice != null
            ) { Text("Quitar límite de precio") }

            // ── Distancia (solo donde aplica) ────────────────────────────────
            if (showDistance) {
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Distancia", fontWeight = FontWeight.SemiBold)
                    val km = filters.maxDistanceKm
                    Text(
                        if (km == null) "Sin límite" else "$km km",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!hasLocation) {
                    Text(
                        "Activa la ubicación para filtrar por distancia.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Slider(
                    value = (filters.maxDistanceKm ?: StationFilters.DISTANCE_MAX_KM).toFloat(),
                    onValueChange = { onChange(filters.copy(maxDistanceKm = it.toInt())) },
                    valueRange = StationFilters.DISTANCE_MIN_KM.toFloat()..StationFilters.DISTANCE_MAX_KM.toFloat(),
                    steps = StationFilters.DISTANCE_MAX_KM - StationFilters.DISTANCE_MIN_KM - 1,
                    enabled = hasLocation
                )
                TextButton(
                    onClick = { onChange(filters.copy(maxDistanceKm = null)) },
                    enabled = hasLocation && filters.maxDistanceKm != null
                ) { Text("Quitar límite de distancia") }
            }

            HorizontalDivider()

            // ── Conmutadores ─────────────────────────────────────────────────
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
            if (showOnlyFavoritesToggle) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Solo favoritas", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = filters.onlyFavorites,
                        onCheckedChange = { onChange(filters.copy(onlyFavorites = it)) }
                    )
                }
            }

            // ── Marca ────────────────────────────────────────────────────────
            if (availableBrands.isNotEmpty()) {
                HorizontalDivider()
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
