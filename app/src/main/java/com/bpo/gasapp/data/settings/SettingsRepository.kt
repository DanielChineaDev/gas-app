package com.bpo.gasapp.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
            dynamicColor = prefs[KEY_DYNAMIC] ?: true
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

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_FUEL = stringPreferencesKey("default_fuel")
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_done")
        val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
    }
}
