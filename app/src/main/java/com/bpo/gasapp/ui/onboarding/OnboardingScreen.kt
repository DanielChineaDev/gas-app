package com.bpo.gasapp.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

private data class TourPage(val emoji: String, val title: String, val body: String)

private val PAGES = listOf(
    TourPage(
        "🗺️",
        "Encuentra tu gasolinera",
        "Lista, mapa con clústeres y precios oficiales en tiempo real de todas las gasolineras de España."
    ),
    TourPage(
        "💚",
        "Guarda tus favoritas",
        "Pulsa el corazón para sincronizarlas con tu cuenta y recibir alertas si bajan de precio."
    ),
    TourPage(
        "📊",
        "Estadísticas y consumo",
        "Registra repostajes con un toque o desde la foto del ticket y mira tu gasto y consumo por mes."
    ),
    TourPage(
        "🚗",
        "Modo coche y ruta",
        "El precio en grande para mirar al volante y un planificador para repostar de paso en tus viajes."
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    var fuel by remember { mutableStateOf(FuelType.GASOLINA_95) }
    val pagerState = rememberPagerState(pageCount = { PAGES.size + 1 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < PAGES.size) {
                TextButton(onClick = { viewModel.completeOnboarding(fuel) }) {
                    Text("Saltar")
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            if (page < PAGES.size) {
                TourPageContent(PAGES[page])
            } else {
                FuelPickerPage(fuel = fuel, onFuelChange = { fuel = it })
            }
        }

        PageIndicator(
            count = PAGES.size + 1,
            current = pagerState.currentPage,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (pagerState.currentPage < PAGES.size) {
            Button(
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Siguiente") }
        } else {
            Button(
                onClick = { viewModel.completeOnboarding(fuel) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Empezar") }
        }
    }
}

@Composable
private fun TourPageContent(page: TourPage) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(page.emoji, style = MaterialTheme.typography.displayLarge)
        Text(
            page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            page.body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FuelPickerPage(fuel: FuelType, onFuelChange: (FuelType) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⛽", style = MaterialTheme.typography.displayLarge)
        Text(
            "¿Qué combustible usas?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            "Lo guardamos como tu favorito. Puedes cambiarlo luego.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FuelType.entries.forEach { f ->
                FilterChip(
                    selected = f == fuel,
                    onClick = { onFuelChange(f) },
                    label = { Text(f.label) }
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == current) 10.dp else 8.dp)
                    .background(
                        color = if (i == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
            )
        }
    }
}
