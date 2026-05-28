package com.bpo.gasapp.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bpo.gasapp.domain.model.MonthlyStat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onAddRefuel: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (state.refuels.isNotEmpty()) {
                        IconButton(onClick = { exportRefuelsCsv(context, state.refuels) }) {
                            Icon(Icons.Default.Download, contentDescription = "Exportar CSV")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddRefuel,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Repostaje") }
            )
        }
    ) { padding ->
        if (state.months.isEmpty()) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Aún no has registrado repostajes.\nUsa el botón + o responde al aviso al llegar a una gasolinera.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.selectedVehicle?.let { vehicle ->
                    item(key = "scope-toggle") {
                        ScopeToggle(
                            vehicleName = vehicle.name,
                            showAll = state.showAllVehicles,
                            onChange = viewModel::setShowAllVehicles
                        )
                    }
                }
                if (state.avgConsumption != null || state.avgCostPerKm != null) {
                    item(key = "consumption") {
                        ConsumptionCard(state.avgConsumption, state.avgCostPerKm)
                    }
                }
                item(key = "calendar") { MonthCalendar(state.dailyAmounts) }
                item(key = "chart") { SpendChart(state.months) }
                items(state.months, key = { it.month }) { month ->
                    MonthCard(month)
                }
            }
        }
    }
}

@Composable
private fun ScopeToggle(vehicleName: String, showAll: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleChip("Solo $vehicleName", !showAll, Modifier.weight(1f)) { onChange(false) }
            ToggleChip("Todos los coches", showAll, Modifier.weight(1f)) { onChange(true) }
        }
    }
}

@Composable
private fun ToggleChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        color = container,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            color = content,
            style = MaterialTheme.typography.labelLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun MonthCalendar(dailyAmounts: Map<String, Double>) {
    var monthOffset by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val firstDay = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        add(java.util.Calendar.MONTH, monthOffset)
    }
    val year = firstDay.get(java.util.Calendar.YEAR)
    val month0 = firstDay.get(java.util.Calendar.MONTH)
    val daysInMonth = firstDay.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    // Lunes=0 ... Domingo=6 (en Calendar, DAY_OF_WEEK: 1=Sun..7=Sat)
    val firstWeekday = (firstDay.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    val monthKeyFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val monthLabel = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("es", "ES"))
        .format(firstDay.time)
        .replaceFirstChar { it.uppercase() }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(onClick = { monthOffset-- }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Mes anterior"
                    )
                }
                Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { monthOffset++ }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Mes siguiente"
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            val totalCells = firstWeekday + daysInMonth
            val rows = (totalCells + 6) / 7
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val cell = r * 7 + c
                        val day = cell - firstWeekday + 1
                        if (day in 1..daysInMonth) {
                            val cal = java.util.Calendar.getInstance().apply {
                                set(year, month0, day)
                            }
                            val key = monthKeyFmt.format(cal.time)
                            val amount = dailyAmounts[key]
                            DayCell(day = day, amount = amount, modifier = Modifier.weight(1f))
                        } else {
                            Box(Modifier.weight(1f).height(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, amount: Double?, modifier: Modifier) {
    Box(
        modifier = modifier.height(40.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(day.toString(), style = MaterialTheme.typography.labelMedium)
            if (amount != null) {
                Text(
                    "%.0f€".format(amount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConsumptionCard(consumption: Double?, costPerKm: Double?) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(
                    consumption?.let { "%.1f".format(it) } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("L/100 km", style = MaterialTheme.typography.labelMedium)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(
                    costPerKm?.let { "%.3f €".format(it) } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("coste/km", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SpendChart(months: List<MonthlyStat>) {
    if (months.isEmpty()) return
    val data = months.sortedBy { it.month }.takeLast(6)
    val max = data.maxOf { it.totalAmount }.takeIf { it > 0 } ?: 1.0
    val barColor = MaterialTheme.colorScheme.primary
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Gasto por mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.Bottom
            ) {
                data.forEach { m ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text("%.0f".format(m.totalAmount), style = MaterialTheme.typography.labelSmall)
                        val barHeight = (90.0 * (m.totalAmount / max)).dp.coerceAtLeast(4.dp)
                        Canvas(modifier = Modifier.fillMaxWidth().height(barHeight)) {
                            drawRect(barColor)
                        }
                        Text(m.month.takeLast(2), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCard(stat: MonthlyStat) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                monthLabel(stat.month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gasto", style = MaterialTheme.typography.bodyMedium)
                Text("%.2f €".format(stat.totalAmount), fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Litros", style = MaterialTheme.typography.bodyMedium)
                Text("%.2f L".format(stat.totalLiters))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Repostajes", style = MaterialTheme.typography.bodyMedium)
                Text(stat.count.toString())
            }
        }
    }
}

private fun monthLabel(monthKey: String): String = runCatching {
    val parsed = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthKey)
    SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(parsed ?: Date())
        .replaceFirstChar { it.uppercase() }
}.getOrDefault(monthKey)
