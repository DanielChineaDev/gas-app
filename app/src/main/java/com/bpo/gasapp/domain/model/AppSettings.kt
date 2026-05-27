package com.bpo.gasapp.domain.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultFuel: FuelType = FuelType.GASOLINA_95,
    val onboardingDone: Boolean = false,
    val dynamicColor: Boolean = true
)
