package com.bpo.gasapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** Degradado animado (shimmer) para los placeholders de carga. */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val base = MaterialTheme.colorScheme.onSurface
    val colors = listOf(
        base.copy(alpha = 0.08f),
        base.copy(alpha = 0.18f),
        base.copy(alpha = 0.08f)
    )
    return Brush.linearGradient(
        colors = colors,
        start = Offset(translate - 500f, 0f),
        end = Offset(translate, 0f)
    )
}

/** Rectángulo redondeado con shimmer (un "hueso" del esqueleto). */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush(),
    cornerRadius: Int = 8
) {
    Box(modifier.clip(RoundedCornerShape(cornerRadius.dp)).background(brush))
}

/** Tarjeta de gasolinera simulada (imita la disposición real de StationCard). */
@Composable
private fun SkeletonStationCard(brush: Brush) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBox(Modifier.size(44.dp), brush, cornerRadius = 11)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonBox(Modifier.fillMaxWidth(0.6f).height(15.dp), brush)
                SkeletonBox(Modifier.fillMaxWidth(0.4f).height(12.dp), brush)
                SkeletonBox(Modifier.fillMaxWidth(0.3f).height(12.dp), brush)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonBox(Modifier.width(64.dp).height(22.dp), brush)
                SkeletonBox(Modifier.width(40.dp).height(12.dp), brush)
            }
        }
    }
}

/**
 * Esqueleto de carga para las listas de gasolineras (Inicio y Favoritas).
 * Muestra varias tarjetas simuladas con animación shimmer mientras se cargan
 * los datos reales.
 */
@Composable
fun StationListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 7,
    showHero: Boolean = true
) {
    val brush = rememberShimmerBrush()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        if (showHero) {
            item { SkeletonBox(Modifier.fillMaxWidth().height(120.dp), brush, cornerRadius = 16) }
        }
        items(itemCount) { SkeletonStationCard(brush) }
    }
}
