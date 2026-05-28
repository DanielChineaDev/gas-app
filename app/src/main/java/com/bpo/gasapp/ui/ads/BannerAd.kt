package com.bpo.gasapp.ui.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bpo.gasapp.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner de AdMob. Se oculta automáticamente cuando el usuario es premium.
 * Coloca este composable allá donde quieras mostrar anuncios; el ViewModel
 * inyectado decide si dibujarlo o no.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val viewModel: BannerAdViewModel = hiltViewModel()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    if (isPremium) return

    val context = LocalContext.current
    AndroidView(
        modifier = modifier.fillMaxWidth().height(50.dp),
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_UNIT
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
