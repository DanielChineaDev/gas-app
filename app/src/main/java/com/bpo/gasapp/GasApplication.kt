package com.bpo.gasapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bpo.gasapp.notifications.PriceNotifier
import com.bpo.gasapp.work.PriceRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GasApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var priceNotifier: PriceNotifier

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        priceNotifier.createChannel()
        PriceRefreshWorker.schedule(this)
        com.google.android.gms.ads.MobileAds.initialize(this) {}
    }
}
