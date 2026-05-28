package com.bpo.gasapp.ui.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.billing.BillingRepository
import com.bpo.gasapp.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PremiumUiState(
    val isPremium: Boolean = false,
    val priceLabel: String? = null,
    val available: Boolean = false
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    init { billingRepository.refresh() }

    val uiState: StateFlow<PremiumUiState> = kotlinx.coroutines.flow.combine(
        settingsRepository.settings.map { it.isPremium },
        billingRepository.productDetails
    ) { isPremium, details ->
        val offer = details?.oneTimePurchaseOfferDetails
        PremiumUiState(
            isPremium = isPremium,
            priceLabel = offer?.formattedPrice,
            available = details != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PremiumUiState())

    fun buy(activity: Activity) {
        billingRepository.launchPurchase(activity)
    }
}
