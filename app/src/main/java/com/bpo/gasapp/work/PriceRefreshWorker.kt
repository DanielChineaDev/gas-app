package com.bpo.gasapp.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bpo.gasapp.domain.repository.StationRepository
import com.bpo.gasapp.notifications.PriceNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class PriceRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: StationRepository,
    private val notifier: PriceNotifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = repository.refreshAndDetectFavoriteDrops()
        return result.fold(
            onSuccess = { drops ->
                notifier.notifyDrops(drops)
                Result.success()
            },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        private const val WORK_NAME = "price_refresh_periodic"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PriceRefreshWorker>(8, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
