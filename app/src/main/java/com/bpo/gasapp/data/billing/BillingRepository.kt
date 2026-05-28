package com.bpo.gasapp.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.bpo.gasapp.BuildConfig
import com.bpo.gasapp.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona la compra única "remove_ads" con Google Play Billing.
 *
 * - Estado premium persistido en DataStore (vía SettingsRepository).
 * - Se conecta a Play Billing al primer uso, restaura compras y se desuscribe
 *   automáticamente cuando termina (sin lifecycle estricto, suficiente para
 *   una compra puntual).
 */
@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val productId: String = BuildConfig.BILLING_REMOVE_ADS_PRODUCT

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private var connecting = false

    private fun ensureConnected(onReady: () -> Unit) {
        if (client.isReady) {
            onReady(); return
        }
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() { connecting = false }
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady()
            }
        })
    }

    fun refresh() {
        ensureConnected {
            queryProductDetails()
            queryExistingPurchases()
        }
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()
        client.queryProductDetailsAsync(params) { _, list ->
            _productDetails.value = list.firstOrNull()
        }
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { _, purchases ->
            if (purchases.any { it.products.contains(productId) && it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                scope.launch { settingsRepository.setPremium(true) }
                purchases.forEach { acknowledgeIfNeeded(it) }
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = _productDetails.value ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        ensureConnected { client.launchBillingFlow(activity, params) }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return
        purchases?.forEach { purchase ->
            if (purchase.products.contains(productId) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                scope.launch { settingsRepository.setPremium(true) }
                acknowledgeIfNeeded(purchase)
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { /* swallow */ }
    }
}
