package com.bpo.gasapp.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bpo.gasapp.domain.model.AppSettings
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            defaultFuel = prefs[KEY_FUEL]?.let { runCatching { FuelType.valueOf(it) }.getOrNull() }
                ?: FuelType.GASOLINA_95,
            onboardingDone = prefs[KEY_ONBOARDING] ?: false,
            dynamicColor = prefs[KEY_DYNAMIC] ?: true,
            alertFuel = prefs[KEY_ALERT_FUEL]?.let { runCatching { FuelType.valueOf(it) }.getOrNull() }
                ?: FuelType.GASOLINA_95,
            alertThreshold = prefs[KEY_ALERT_THRESHOLD],
            selectedVehicleId = prefs[KEY_SELECTED_VEHICLE],
            isPremium = prefs[KEY_PREMIUM] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setDefaultFuel(fuel: FuelType) {
        dataStore.edit { it[KEY_FUEL] = fuel.name }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING] = done }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC] = enabled }
    }

    suspend fun setPremium(value: Boolean) {
        dataStore.edit { it[KEY_PREMIUM] = value }
    }

    suspend fun setSelectedVehicle(id: Long?) {
        dataStore.edit {
            if (id == null) it.remove(KEY_SELECTED_VEHICLE) else it[KEY_SELECTED_VEHICLE] = id
        }
    }

    suspend fun setPriceAlert(fuel: FuelType, threshold: Double?) {
        dataStore.edit {
            it[KEY_ALERT_FUEL] = fuel.name
            if (threshold == null) it.remove(KEY_ALERT_THRESHOLD) else it[KEY_ALERT_THRESHOLD] = threshold
        }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_FUEL = stringPreferencesKey("default_fuel")
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_done")
        val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
        val KEY_ALERT_FUEL = stringPreferencesKey("alert_fuel")
        val KEY_ALERT_THRESHOLD = doublePreferencesKey("alert_threshold")
        val KEY_SELECTED_VEHICLE = longPreferencesKey("selected_vehicle")
        val KEY_PREMIUM = booleanPreferencesKey("is_premium")
    }
}
