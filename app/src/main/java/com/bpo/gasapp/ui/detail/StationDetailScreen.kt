package com.bpo.gasapp.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDetailScreen(
    onBack: () -> Unit,
    viewModel: StationDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.station?.brand ?: "Detalle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    state.station?.let { st ->
                        IconButton(onClick = {
                            com.bpo.gasapp.ui.components.shareStation(context, st)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir")
                        }
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (st.isFavorite) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorito"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.notFound || state.station == null ->
                    Text(
                        "Gasolinera no encontrada.",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                else -> StationDetailContent(
                    station = state.station!!,
                    history = history,
                    onNavigate = { launchNavigation(context, state.station!!) }
                )
            }
        }
    }
}

@Composable
private fun StationDetailContent(
    station: Station,
    history: List<com.bpo.gasapp.domain.model.PricePoint>,
    onNavigate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StationPhoto(station)
        Column {
            Text(station.brand, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (station.address.isNotBlank()) Text(station.address, style = MaterialTheme.typography.bodyMedium)
            val place = listOf(station.city, station.province).filter { it.isNotBlank() }.joinToString(", ")
            if (place.isNotBlank()) Text(place, style = MaterialTheme.typography.bodyMedium)
            if (station.schedule.isNotBlank()) {
                Text("Horario: ${station.schedule}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Precios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FuelType.entries.forEach { fuel ->
                    PriceRow(fuel, station.priceOf(fuel))
                    if (fuel != FuelType.entries.last()) HorizontalDivider()
                }
            }
        }

        ServicesSection(station)

        PriceHistorySection(history)

        Button(onClick = onNavigate, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Navigation, contentDescription = null)
            Text("  Ir allí")
        }

        com.bpo.gasapp.ui.ads.BannerAd()
    }
}

@Composable
private fun ReviewInput(onSubmit: (Int, String) -> Unit) {
    var rating by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var comment by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Row {
        (1..5).forEach { star ->
            IconButton(onClick = { rating = star }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "$star estrellas",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    androidx.compose.material3.OutlinedTextField(
        value = comment,
        onValueChange = { comment = it },
        label = { Text("Tu comentario (opcional)") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = { onSubmit(rating, comment) },
        enabled = rating > 0,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Publicar reseña") }
}

@Composable
private fun StationPhoto(station: Station) {
    val url = "https://maps.googleapis.com/maps/api/streetview?size=600x300" +
        "&location=${station.latitude},${station.longitude}&fov=80&pitch=0" +
        "&key=${com.bpo.gasapp.BuildConfig.MAPS_API_KEY}"

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(com.bpo.gasapp.ui.components.brandColor(station.brand)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = station.brand.take(1).uppercase(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White
        )
        coil.compose.AsyncImage(
            model = url,
            contentDescription = "Foto de ${station.brand}",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(180.dp)
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ServicesSection(station: Station) {
    val services = buildList {
        if (station.priceOf(FuelType.ADBLUE) != null) add("AdBlue")
        if (station.priceOf(FuelType.GLP) != null) add("GLP / Autogás")
        if (station.priceOf(FuelType.GNC) != null) add("Gas natural (GNC)")
        if (station.priceOf(FuelType.GNL) != null) add("Gas natural (GNL)")
        if (station.priceOf(FuelType.HIDROGENO) != null) add("Hidrógeno")
        if (station.schedule.uppercase().replace(" ", "").contains("24H")) add("Abierto 24 h")
    }
    if (services.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Servicios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                services.forEach { s ->
                    androidx.compose.material3.AssistChip(onClick = {}, label = { Text(s) })
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PriceHistorySection(history: List<com.bpo.gasapp.domain.model.PricePoint>) {
    val ranges = listOf(7 to "7 días", 30 to "30 días", 90 to "90 días", 365 to "1 año")
    var rangeDays by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(30) }
    val presentFuels = androidx.compose.runtime.remember(history) { history.map { it.fuel }.distinct() }
    var enabled by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Set<FuelType>>(emptySet()) }
    androidx.compose.runtime.LaunchedEffect(presentFuels) {
        if (enabled.isEmpty() && presentFuels.isNotEmpty()) enabled = presentFuels.toSet()
    }
    val cutoff = System.currentTimeMillis() - rangeDays.toLong() * 86_400_000L
    val filtered = history.filter { it.timestamp >= cutoff }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Histórico de precios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ranges.forEach { (d, label) ->
                    androidx.compose.material3.FilterChip(
                        selected = rangeDays == d,
                        onClick = { rangeDays = d },
                        label = { Text(label) }
                    )
                }
            }
            if (presentFuels.size > 1) {
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presentFuels.forEach { f ->
                        androidx.compose.material3.FilterChip(
                            selected = f in enabled,
                            onClick = { enabled = if (f in enabled) enabled - f else enabled + f },
                            label = { Text(f.label) },
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(fuelColors[f] ?: androidx.compose.ui.graphics.Color.Gray)
                                )
                            }
                        )
                    }
                }
            }
            PriceHistoryChart(filtered, enabled)
        }
    }
}

@Composable
private fun PriceRow(fuel: FuelType, price: Double?) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(fuel.label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = price?.let { "%.3f €".format(it) } ?: "No disponible",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun launchNavigation(context: android.content.Context, station: Station) {
    com.bpo.gasapp.ui.components.openNavigation(context, station.latitude, station.longitude)
}
