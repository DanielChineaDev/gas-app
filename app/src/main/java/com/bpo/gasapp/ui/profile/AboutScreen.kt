package com.bpo.gasapp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun openUrl(url: String) {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)
        )
        kotlin.runCatching { context.startActivity(intent) }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sobre la aplicación") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AboutBlock(
                "Qué es GasApp",
                "GasApp es una app para encontrar las gasolineras más baratas de España en " +
                    "tiempo real, comparar precios de carburante y ahorrar en cada repostaje."
            )
            AboutBlock(
                "Objetivo",
                "Ayudarte a pagar menos por el combustible mostrándote, de forma rápida y clara, " +
                    "dónde repostar más barato cerca de ti."
            )
            AboutBlock(
                "Qué te aporta",
                "• Mapa y lista con precios oficiales actualizados a diario.\n" +
                    "• Favoritas sincronizadas y alertas de bajada de precio.\n" +
                    "• Estadísticas de gasto, consumo y ahorro.\n" +
                    "• Histórico de precios, modo coche y mucho más."
            )
            AboutBlock(
                "Desarrollador",
                "Desarrollado por Jose Daniel Chinea (BPO Studios)."
            )
            AboutBlock(
                "Datos y privacidad",
                "Los precios proceden de fuentes oficiales públicas. La ubicación se usa solo para " +
                    "mostrarte gasolineras cercanas y no se rastrea en segundo plano. La cuenta es " +
                    "opcional y no vendemos tus datos."
            )
            AboutBlock(
                "Aviso legal",
                "Los nombres, marcas y logotipos pertenecen a sus respectivos propietarios y se " +
                    "muestran únicamente como identificador de cada estación de servicio. Esta " +
                    "aplicación no está afiliada, asociada ni respaldada oficialmente por ninguna " +
                    "compañía petrolera."
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { openUrl("https://landing.gasapp.cloud") }) {
                    Text("Sitio web")
                }
                TextButton(onClick = { openUrl("https://landing.gasapp.cloud/privacy.html") }) {
                    Text("Privacidad")
                }
                TextButton(onClick = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_SENDTO,
                        android.net.Uri.parse("mailto:info@gasapp.cloud")
                    )
                    kotlin.runCatching { context.startActivity(intent) }
                }) {
                    Text("Contacto")
                }
            }

            Text(
                "Versión ${com.bpo.gasapp.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AboutBlock(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
