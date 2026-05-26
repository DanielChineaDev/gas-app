package com.bpo.gasapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bpo.gasapp.R
import com.bpo.gasapp.domain.model.PriceDrop
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bajadas de precio",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos cuando baja el precio en tus gasolineras favoritas" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun notifyDrops(drops: List<PriceDrop>) {
        if (drops.isEmpty() || !hasPermission()) return

        val title = if (drops.size == 1) "Bajada de precio" else "${drops.size} bajadas de precio"
        val text = drops.joinToString(" · ") {
            "${it.stationName}: ${it.fuel.label} %.3f €".format(it.newPrice)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ID = "price_drops"
        private const val NOTIFICATION_ID = 1001
    }
}
