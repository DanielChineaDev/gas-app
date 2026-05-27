package com.bpo.gasapp.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.remote.ReviewRemoteDataSource
import com.bpo.gasapp.domain.model.PricePoint
import com.bpo.gasapp.domain.model.Review
import com.bpo.gasapp.domain.model.Station
import com.bpo.gasapp.domain.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StationDetailUiState(
    val station: Station? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false
)

@HiltViewModel
class StationDetailViewModel @Inject constructor(
    private val repository: StationRepository,
    private val reviewRemote: ReviewRemoteDataSource,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val stationId: String = checkNotNull(savedStateHandle[StationDetailRoute.ARG_ID])

    private val _uiState = MutableStateFlow(StationDetailUiState())
    val uiState: StateFlow<StationDetailUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<PricePoint>> =
        repository.observePriceHistory(stationId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val reviews: StateFlow<List<Review>> =
        reviewRemote.observeReviews(stationId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val canReview: Boolean get() = reviewRemote.isLoggedIn()

    fun submitReview(rating: Int, comment: String) {
        viewModelScope.launch { reviewRemote.submitReview(stationId, rating, comment) }
    }

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val station = repository.getStation(stationId)
            _uiState.value = StationDetailUiState(
                station = station,
                isLoading = false,
                notFound = station == null
            )
        }
    }

    fun toggleFavorite() {
        val current = _uiState.value.station ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current.id)
            _uiState.value = _uiState.value.copy(
                station = current.copy(isFavorite = !current.isFavorite)
            )
        }
    }
}
