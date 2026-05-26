package com.bpo.gasapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station

private fun formatDistance(meters: Float): String =
    if (meters < 1000) "a ${meters.toInt()} m"
    else "a %.1f km".format(meters / 1000f)

@Composable
fun StationCard(
    station: Station,
    fuel: FuelType,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    isCheapest: Boolean = false,
    modifier: Modifier = Modifier
) {
    val price = station.priceOf(fuel)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = if (isCheapest) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    station.brand,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    station.address.ifBlank { station.city },
                    style = MaterialTheme.typography.bodySmall
                )
                station.distanceMeters?.let { meters ->
                    Text(
                        text = formatDistance(meters),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (isCheapest) {
                    Text(
                        "Más barata",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = price?.let { "%.3f €".format(it) } ?: "—",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito"
                )
            }
        }
    }
}
