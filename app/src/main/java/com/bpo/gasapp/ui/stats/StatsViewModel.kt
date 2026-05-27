package com.bpo.gasapp.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.domain.model.MonthlyStat
import com.bpo.gasapp.domain.model.Refuel
import com.bpo.gasapp.domain.repository.RefuelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StatsUiState(
    val months: List<MonthlyStat> = emptyList(),
    val refuels: List<Refuel> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: RefuelRepository
) : ViewModel() {

    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    val uiState: StateFlow<StatsUiState> =
        repository.observeRefuels().map { refuels ->
            val months = refuels
                .groupBy { monthKeyFormat.format(Date(it.timestamp)) }
                .map { (month, list) ->
                    MonthlyStat(
                        month = month,
                        totalAmount = list.sumOf { it.amount },
                        totalLiters = list.sumOf { it.liters },
                        count = list.size
                    )
                }
                .sortedByDescending { it.month }
            StatsUiState(months = months, refuels = refuels)
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState()
        )

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
