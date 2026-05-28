package com.bpo.gasapp.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.model.Achievement
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.repository.RefuelRepository
import com.bpo.gasapp.domain.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AchievementsUiState(
    val moneySaved: Double = 0.0,
    val refuelCount: Int = 0,
    val favoritesCount: Int = 0,
    val achievements: List<Achievement> = emptyList()
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    refuelRepository: RefuelRepository,
    stationRepository: StationRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> =
        combine(
            refuelRepository.observeRefuels(),
            stationRepository.observeStations(),
            stationRepository.observeFavorites(),
            settingsRepository.settings
        ) { refuels, stations, favorites, settings ->
            val avgByFuel = FuelType.entries.associateWith { fuel ->
                val prices = stations.mapNotNull { it.priceOf(fuel) }
                if (prices.isNotEmpty()) prices.average() else null
            }
            val saved = refuels.sumOf { r ->
                val avg = avgByFuel[r.fuel]
                val price = r.pricePerLiter
                if (avg != null && price != null && avg > price) (avg - price) * r.liters else 0.0
            }
            val refuelCount = refuels.size
            val favCount = favorites.size

            AchievementsUiState(
                moneySaved = saved,
                refuelCount = refuelCount,
                favoritesCount = favCount,
                achievements = buildAchievements(saved, refuelCount, favCount, settings.isPremium)
            )
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AchievementsUiState()
        )

    private fun buildAchievements(
        saved: Double,
        refuelCount: Int,
        favCount: Int,
        isPremium: Boolean
    ): List<Achievement> {
        fun progress(value: Number, goal: Number): Float =
            (value.toDouble() / goal.toDouble()).coerceIn(0.0, 1.0).toFloat()

        return listOf(
            Achievement(
                id = "first_refuel",
                emoji = "⛽",
                title = "Primer repostaje",
                description = "Registra tu primer repostaje.",
                unlocked = refuelCount >= 1,
                progress = progress(refuelCount, 1)
            ),
            Achievement(
                id = "regular",
                emoji = "🔁",
                title = "Repostador habitual",
                description = "Registra 10 repostajes.",
                unlocked = refuelCount >= 10,
                progress = progress(refuelCount, 10)
            ),
            Achievement(
                id = "collector",
                emoji = "💚",
                title = "Coleccionista",
                description = "Guarda 5 gasolineras favoritas.",
                unlocked = favCount >= 5,
                progress = progress(favCount, 5)
            ),
            Achievement(
                id = "saver_10",
                emoji = "🪙",
                title = "Ahorrador",
                description = "Ahorra 10 € repostando por debajo de la media.",
                unlocked = saved >= 10,
                progress = progress(saved, 10)
            ),
            Achievement(
                id = "saver_50",
                emoji = "💰",
                title = "Gran ahorrador",
                description = "Ahorra 50 € usando la app.",
                unlocked = saved >= 50,
                progress = progress(saved, 50)
            ),
            Achievement(
                id = "saver_100",
                emoji = "🏆",
                title = "Maestro del ahorro",
                description = "Ahorra 100 € usando la app.",
                unlocked = saved >= 100,
                progress = progress(saved, 100)
            ),
            Achievement(
                id = "supporter",
                emoji = "⭐",
                title = "Mecenas",
                description = "Apoya el proyecto quitando los anuncios.",
                unlocked = isPremium,
                progress = if (isPremium) 1f else 0f
            )
        )
    }
}
