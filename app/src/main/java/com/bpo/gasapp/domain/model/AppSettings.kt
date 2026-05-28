package com.bpo.gasapp.domain.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultFuel: FuelType = FuelType.GASOLINA_95,
    val onboardingDone: Boolean = false,
    val dynamicColor: Boolean = true,
    /** Price alert: notify if any nearby station for this fuel drops to/below the threshold. */
    val alertFuel: FuelType = FuelType.GASOLINA_95,
    val alertThreshold: Double? = null,
    val selectedVehicleId: Long? = null,
    val isPremium: Boolean = false
)
