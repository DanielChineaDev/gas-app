package com.bpo.gasapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.bpo.gasapp.MainActivity
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class GasWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, WidgetEntryPoint::class.java
        )
        val settings = entryPoint.settingsRepository().settings.first()
        val fuel = settings.defaultFuel
        val favorites = entryPoint.stationRepository().observeFavorites().first()
            .mapNotNull { station -> station.priceOf(fuel)?.let { station to it } }
            .sortedBy { it.second }
            .take(4)

        provideContent {
            GlanceTheme {
                WidgetContent(fuel = fuel, items = favorites)
            }
        }
    }
}

@Composable
private fun WidgetContent(fuel: FuelType, items: List<Pair<Station, Double>>) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "Favoritas más baratas · ${fuel.label}",
            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
        )
        if (items.isEmpty()) {
            Text(
                "Sin favoritas con precio",
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface)
            )
        } else {
            items.forEach { (station, price) ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        station.brand,
                        maxLines = 1,
                        style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        "%.3f €".format(price),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GlanceTheme.colors.primary
                        )
                    )
                }
            }
        }
    }
}
