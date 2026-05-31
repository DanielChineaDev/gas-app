package com.bpo.gasapp.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Mide el arranque en frío de GasApp con y sin Baseline Profile, para comprobar
 * la mejora real. Ejecuta (con dispositivo/emulador):
 *   ./gradlew :baselineprofile:connectedBenchmarkAndroidTest
 * o desde Android Studio pulsando el "play" junto a cada test.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupSinCompilacion() = startup(CompilationMode.None())

    @Test
    fun startupConBaselineProfile() =
        startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = "com.bpo.gasapp",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        compilationMode = mode
    ) {
        pressHome()
        startActivityAndWait()
    }
}
