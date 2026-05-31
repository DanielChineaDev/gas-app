package com.bpo.gasapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.bpo.gasapp.domain.model.ThemeMode
import com.bpo.gasapp.ui.navigation.GasNavHost
import com.bpo.gasapp.ui.onboarding.OnboardingScreen
import com.bpo.gasapp.ui.settings.SettingsViewModel
import com.bpo.gasapp.ui.theme.GasAppTheme
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalPermissionsApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        // Salida elegante: el icono se desvanece y crece ligeramente.
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(280L)
                .withEndAction { provider.remove() }
                .start()
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Consentimiento de privacidad (RGPD/UMP) antes de inicializar AdMob.
        com.bpo.gasapp.ui.ads.AdsConsentManager.gatherConsentAndInitialize(this)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val onboardingDone by settingsViewModel.onboardingDone.collectAsStateWithLifecycle()

            splash.setKeepOnScreenCondition { onboardingDone == null }

            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            GasAppTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                // Pedir permiso de notificaciones (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }
                    LaunchedEffect(Unit) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Pedir permiso de ubicación automáticamente después del onboarding
                val locationPermission = rememberMultiplePermissionsState(
                    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
                LaunchedEffect(onboardingDone) {
                    if (onboardingDone == true && !locationPermission.allPermissionsGranted) {
                        locationPermission.launchMultiplePermissionRequest()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (onboardingDone) {
                        null -> Unit
                        false -> OnboardingScreen()
                        true -> GasNavHost()
                    }
                }
            }
        }
    }
}
