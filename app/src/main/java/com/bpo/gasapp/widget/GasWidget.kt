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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        horizontalAlignment = Alignment.Start
    ) {
        // Cabecera de marca.
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(22.dp)
                    .cornerRadius(7.dp)
                    .background(BrandBlue),
                contentAlignment = Alignment.Center
            ) {
                Text("⛽", style = TextStyle(fontSize = 11.sp, color = ColorProvider(WhiteColor)))
            }
            Spacer(GlanceModifier.width(8.dp))
            Text(
                "GasApp",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ColorProvider(BrandBlue))
            )
            Text(
                "  ·  Favoritas",
                style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                fuel.label,
                style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        if (items.isEmpty()) {
            Text(
                "Añade gasolineras favoritas para verlas aquí.",
                style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
        } else {
            items.forEach { (station, price) ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(30.dp)
                            .cornerRadius(8.dp)
                            .background(com.bpo.gasapp.ui.components.brandColor(station.brand)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            station.brand.trim().take(1).uppercase().ifBlank { "?" },
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ColorProvider(WhiteColor))
                        )
                    }
                    Spacer(GlanceModifier.width(10.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            station.brand,
                            maxLines = 1,
                            style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = GlanceTheme.colors.onSurface)
                        )
                        if (station.city.isNotBlank()) {
                            Text(
                                station.city,
                                maxLines = 1,
                                style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant)
                            )
                        }
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        "%.3f €".format(price),
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorProvider(BrandBlue))
                    )
                }
            }
        }
    }
}

private val BrandBlue = androidx.compose.ui.graphics.Color(0xFF1976D2)
private val WhiteColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
