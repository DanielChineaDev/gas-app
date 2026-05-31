package com.bpo.gasapp.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Genera el Baseline Profile de GasApp.
 *
 * Ejecuta (con un dispositivo conectado o el emulador gestionado):
 *   ./gradlew :app:generateBaselineProfile
 *
 * El perfil resultante se copia a `app/src/<variant>/generated/baselineProfiles/`
 * y se incluye automáticamente en el AAB de release (vía ProfileInstaller),
 * lo que acelera el arranque en frío y reduce el "jank" del primer uso.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.bpo.gasapp",
        // Incluye también el "startup profile" (optimiza específicamente el arranque).
        includeInStartupProfile = true
    ) {
        // Arranque en frío hasta la primera pantalla.
        pressHome()
        startActivityAndWait()

        // Da tiempo a que cargue el contenido inicial (lista de gasolineras) y
        // hace un par de desplazamientos para capturar el código del scroll.
        device.waitForIdle()
        repeat(2) {
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.7).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.3).toInt(),
                10
            )
            device.waitForIdle()
        }
    }
}
