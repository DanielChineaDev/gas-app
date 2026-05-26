package com.bpo.gasapp.domain.util

import android.location.Location
import com.bpo.gasapp.domain.model.UserLocation

fun distanceMeters(from: UserLocation, lat: Double, lon: Double): Float {
    val result = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, lat, lon, result)
    return result[0]
}
