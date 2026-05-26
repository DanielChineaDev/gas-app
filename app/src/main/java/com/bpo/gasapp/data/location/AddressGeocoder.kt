package com.bpo.gasapp.data.location

import android.content.Context
import android.location.Geocoder
import com.bpo.gasapp.domain.model.UserLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressGeocoder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Resolves a free-text place/address to coordinates, or null if not found. */
    suspend fun geocode(query: String): UserLocation? = withContext(Dispatchers.IO) {
        if (query.isBlank() || !Geocoder.isPresent()) return@withContext null
        runCatching {
            @Suppress("DEPRECATION")
            val results = Geocoder(context, Locale.getDefault()).getFromLocationName(query, 1)
            results?.firstOrNull()?.let { UserLocation(it.latitude, it.longitude) }
        }.getOrNull()
    }
}
