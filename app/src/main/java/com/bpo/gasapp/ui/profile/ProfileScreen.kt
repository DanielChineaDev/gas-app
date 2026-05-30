package com.bpo.gasapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsCarFilled
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bpo.gasapp.ui.account.AccountViewModel
import com.bpo.gasapp.ui.components.LoginRequiredDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogin: () -> Unit,
    onStats: () -> Unit,
    onSaving: () -> Unit,
    onCarMode: () -> Unit,
    onVehicles: () -> Unit,
    onAchievements: () -> Unit,
    onPremium: () -> Unit,
    onSettings: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val favoritesCount by viewModel.favoritesCount.collectAsStateWithLifecycle()
    val moneySaved by viewModel.moneySaved.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showLoginRequired by remember { mutableStateOf(false) }

    val isLoggedIn = user != null

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.statusBars,
        topBar = { TopAppBar(title = { Text("Perfil") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoggedIn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val displayName = user?.displayName?.takeIf { it.isNotBlank() }
                    val initialSource = displayName ?: user?.email
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (initialSource?.take(1) ?: "U").uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            displayName ?: "Hola 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            user?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar nombre")
                    }
                }
            } else {
                SignInCard(onLogin = onLogin)
            }

            // Las estadísticas (favoritas y ahorro) requieren sesión: se mantienen
            // visibles pero atenuadas y bloqueadas si el usuario no ha entrado.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    value = if (isLoggedIn) "$favoritesCount" else "—",
                    label = "Gasolineras favoritas",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    locked = !isLoggedIn,
                    onLockedClick = { showLoginRequired = true },
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = if (isLoggedIn) "%.2f €".format(moneySaved) else "—",
                    label = "Ahorrado con la app",
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    locked = !isLoggedIn,
                    onLockedClick = { showLoginRequired = true },
                    modifier = Modifier.weight(1f)
                )
            }

            SupportCard(
                onKofi = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(KOFI_URL)
                    )
                    kotlin.runCatching { context.startActivity(intent) }
                }
            )

            HorizontalDivider()

            MenuRow(Icons.AutoMirrored.Filled.ListAlt, "Mis estadísticas", onClick = onStats)
            MenuRow(
                icon = Icons.Default.EmojiEvents,
                label = "Mis logros",
                locked = !isLoggedIn,
                onClick = { if (isLoggedIn) onAchievements() else showLoginRequired = true }
            )
            MenuRow(Icons.Default.DirectionsCarFilled, "Mis vehículos", onClick = onVehicles)
            MenuRow(Icons.Default.Savings, "Modo ahorro", onClick = onSaving)
            MenuRow(Icons.Default.DirectionsCar, "Modo coche", onClick = onCarMode)
            MenuRow(Icons.Default.Star, "Quitar anuncios", onClick = onPremium)
            MenuRow(Icons.Default.Settings, "Ajustes", onClick = onSettings)

            HorizontalDivider()

            if (isLoggedIn) {
                OutlinedButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar sesión")
                }
            }

            HorizontalDivider()

            AboutSection(
                onOpenUrl = { url ->
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)
                    )
                    kotlin.runCatching { context.startActivity(intent) }
                },
                onEmail = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_SENDTO,
                        android.net.Uri.parse("mailto:info@gasapp.cloud")
                    )
                    kotlin.runCatching { context.startActivity(intent) }
                }
            )

            VersionFooter()
        }
    }

    if (showEditDialog) {
        EditNameDialog(
            current = user?.displayName.orEmpty(),
            onConfirm = { viewModel.updateDisplayName(it); showEditDialog = false },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showLoginRequired) {
        LoginRequiredDialog(
            onLogin = onLogin,
            onDismiss = { showLoginRequired = false }
        )
    }
}

@Composable
private fun SignInCard(onLogin: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Sincroniza tus datos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Inicia sesión para guardar tus favoritas, tu ahorro y tus logros en la nube.",
                style = MaterialTheme.typography.bodyMedium
            )
            androidx.compose.material3.Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Iniciar sesión o registrarse")
            }
        }
    }
}

@Composable
private fun EditNameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar nombre") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Tu nombre") }
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(name) }) { Text("Guardar") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private const val KOFI_URL = "https://ko-fi.com/josedanielchinea"

@Composable
private fun StatCard(
    value: String,
    label: String,
    container: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    onLockedClick: () -> Unit = {}
) {
    val cardModifier = if (locked) modifier.clickable(onClick = onLockedClick) else modifier
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Box {
            Column(
                Modifier
                    .padding(16.dp)
                    .alpha(if (locked) 0.45f else 1f)
            ) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
            if (locked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Requiere iniciar sesión",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SupportCard(onKofi: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onKofi),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("☕", style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f)) {
                Text(
                    "Apoya el desarrollo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "GasApp es gratuita. Invítame a un café en Ko-fi para seguir mejorándola.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    locked: Boolean = false
) {
    ListItem(
        headlineContent = {
            Text(
                label,
                color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (locked) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
        },
        trailingContent = if (locked) {
            {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Requiere iniciar sesión",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (locked) 0.6f else 1f)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun AboutSection(onOpenUrl: (String) -> Unit, onEmail: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Sobre la aplicación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (expanded) {
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
                    androidx.compose.material3.TextButton(onClick = { onOpenUrl("https://landing.gasapp.cloud") }) {
                        Text("Sitio web")
                    }
                    androidx.compose.material3.TextButton(onClick = { onOpenUrl("https://landing.gasapp.cloud/privacy.html") }) {
                        Text("Privacidad")
                    }
                    androidx.compose.material3.TextButton(onClick = onEmail) {
                        Text("Contacto")
                    }
                }
            }
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

@Composable
private fun VersionFooter() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            "GasApp ${com.bpo.gasapp.BuildConfig.VERSION_NAME} (build ${com.bpo.gasapp.BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Compilada el ${com.bpo.gasapp.BuildConfig.BUILD_DATE}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
