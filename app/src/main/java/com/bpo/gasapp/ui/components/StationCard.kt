package com.bpo.gasapp.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.ui.theme.FavoriteRed

private fun formatDistance(meters: Float): String =
    if (meters < 1000) "${meters.toInt()} m"
    else "%.1f km".format(meters / 1000f)

@Composable
fun StationCard(
    station: Station,
    fuel: FuelType,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    isCheapest: Boolean = false,
    zoneAverage: Double? = null,
    consumptionL100: Double = com.bpo.gasapp.ui.stations.DEFAULT_CONSUMPTION,
    modifier: Modifier = Modifier
) {
    val price = station.priceOf(fuel)
    // Coste estimado de ida hasta la gasolinera con el consumo del vehículo.
    val tripCost: Double? = station.distanceMeters?.let { meters ->
        price?.let { p -> (meters / 1000.0) * (consumptionL100 / 100.0) * p }
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = if (isCheapest) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandLogo(brand = station.brand)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    station.brand,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    station.address.ifBlank { station.city },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (station.distanceMeters != null) {
                        Icon(
                            Icons.Default.NearMe,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        val distanceText = buildString {
                            append("  ${formatDistance(station.distanceMeters)}")
                            if (tripCost != null) append(" • %.2f € para llegar".format(tripCost))
                        }
                        Text(
                            distanceText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (isCheapest) {
                        Text(
                            if (station.distanceMeters != null) "   · Más barata" else "Más barata",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Único corazón, junto al precio: indica y permite alternar favorito.
            val favScale by animateFloatAsState(
                targetValue = if (station.isFavorite) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "favScale"
            )
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (station.isFavorite) FavoriteRed
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.scale(favScale)
                )
            }

            PricePill(price, zoneAverage)
        }
    }
}

private val PriceCheap = Color(0xFF2E7D32)
private val PriceMid = Color(0xFFF59E0B)
private val PriceExpensive = Color(0xFFD32F2F)

@Composable
private fun PricePill(price: Double?, zoneAverage: Double?) {
    val heat = when {
        price == null -> null
        zoneAverage == null -> MaterialTheme.colorScheme.primary
        price <= zoneAverage * 0.99 -> PriceCheap
        price >= zoneAverage * 1.01 -> PriceExpensive
        else -> PriceMid
    }
    val container = heat ?: MaterialTheme.colorScheme.surfaceVariant
    val content = if (heat != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .background(container, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = price?.let { "%.3f".format(it) } ?: "—",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = content
        )
        Text(
            "€/L",
            style = MaterialTheme.typography.labelSmall,
            color = content
        )
    }
}
