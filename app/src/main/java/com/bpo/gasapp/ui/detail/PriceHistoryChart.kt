package com.bpo.gasapp.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.PricePoint

/** Colores por carburante para las series del gráfico. */
internal val fuelColors = mapOf(
    FuelType.GASOLINA_95 to Color(0xFF2E7D32),
    FuelType.GASOLINA_98 to Color(0xFF1565C0),
    FuelType.DIESEL to Color(0xFFEF6C00),
    FuelType.DIESEL_PREMIUM to Color(0xFF6A1B9A),
    FuelType.GLP to Color(0xFF00897B),
    FuelType.GNC to Color(0xFF5D4037),
    FuelType.GNL to Color(0xFF0097A7),
    FuelType.HIDROGENO to Color(0xFFC2185B),
    FuelType.ADBLUE to Color(0xFF546E7A)
)

/**
 * Gráfico de líneas del histórico de precios. El padre filtra por rango temporal
 * y pasa los carburantes activos (para comparar varios a la vez).
 */
@Composable
fun PriceHistoryChart(
    history: List<PricePoint>,
    enabledFuels: Set<FuelType>,
    modifier: Modifier = Modifier
) {
    val byFuel = history
        .filter { it.fuel in enabledFuels }
        .groupBy { it.fuel }
        .mapValues { (_, points) -> points.sortedBy { it.timestamp } }
        .filterValues { it.size >= 2 }

    if (byFuel.isEmpty()) {
        Text(
            "Aún no hay suficiente histórico para este rango. Se va construyendo cada vez " +
                "que abres la gasolinera o se actualizan los precios.",
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier
        )
        return
    }

    val flat = byFuel.values.flatten()
    val minPrice = flat.minOf { it.price }
    val maxPrice = flat.maxOf { it.price }
    val range = (maxPrice - minPrice).takeIf { it > 0.0001 } ?: 1.0
    val minTime = flat.minOf { it.timestamp }
    val maxTime = flat.maxOf { it.timestamp }
    val timeSpan = (maxTime - minTime).takeIf { it > 0 } ?: 1L

    val grid = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp)
        ) {
            // Líneas de referencia horizontales.
            val lines = 3
            for (i in 0..lines) {
                val y = size.height * i / lines
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            byFuel.forEach { (fuel, points) ->
                val color = fuelColors[fuel] ?: Color.Gray
                val offsets = points.map { point ->
                    val x = ((point.timestamp - minTime).toFloat() / timeSpan) * size.width
                    val y = size.height - ((point.price - minPrice).toFloat() / range.toFloat()) * size.height
                    Offset(x, y)
                }
                for (i in 0 until offsets.size - 1) {
                    drawLine(
                        color = color,
                        start = offsets[i],
                        end = offsets[i + 1],
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }
                offsets.forEach { drawCircle(color, radius = 4f, center = it) }
            }
        }

        Text(
            "Mín. %.3f €  ·  Máx. %.3f €".format(minPrice, maxPrice),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
