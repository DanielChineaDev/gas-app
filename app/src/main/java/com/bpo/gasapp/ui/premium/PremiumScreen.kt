package com.bpo.gasapp.ui.premium

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val promo by viewModel.promoState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPromo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quitar anuncios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("🚫📺", style = MaterialTheme.typography.displayMedium)
            Text(
                "Apoya el proyecto y disfruta de GasApp sin publicidad para siempre.",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        state.isPremium -> {
                            Text("¡Gracias!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Estás disfrutando de GasApp sin anuncios.")
                        }
                        state.available -> {
                            Text(state.priceLabel ?: "", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Pago único · sin suscripciones")
                            Button(
                                onClick = { (context as? Activity)?.let { viewModel.buy(it) } },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) { Text("Quitar anuncios") }
                        }
                        else -> {
                            Text(
                                "La compra aún no está disponible.\nVuelve a abrir esta pantalla en unos segundos.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (!state.isPremium) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { showPromo = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("¿Tienes un código?") }
            }

            val coffeeUrl = com.bpo.gasapp.BuildConfig.COFFEE_URL
            if (coffeeUrl.isNotBlank()) {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(coffeeUrl)
                        )
                        kotlin.runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("☕  Invítame a un café") }
            }
        }
    }

    if (showPromo) {
        PromoDialog(
            isRedeeming = promo.isRedeeming,
            message = promo.message,
            onRedeem = viewModel::redeemCode,
            onDismiss = {
                showPromo = false
                viewModel.clearPromoMessage()
            }
        )
    }
}

@Composable
private fun PromoDialog(
    isRedeeming: Boolean,
    message: String?,
    onRedeem: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Canjear código") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    singleLine = true,
                    label = { Text("Código promocional") }
                )
                if (message != null) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onRedeem(code) },
                enabled = !isRedeeming && code.isNotBlank()
            ) { Text(if (isRedeeming) "..." else "Canjear") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
