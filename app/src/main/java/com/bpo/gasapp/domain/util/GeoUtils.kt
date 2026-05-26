package com.bpo.gasapp.domain.util

import android.location.Location
import com.bpo.gasapp.domain.model.UserLocation

fun distanceMeters(from: UserLocation, lat: Double, lon: Double): Float {
    val result = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, lat, lon, result)
    return result[0]
}

/**
 * Approximate distance in meters from a point to the segment a–b, using an
 * equirectangular projection around the segment's mean latitude. Good enough
 * for filtering stations within a corridor of a few km.
 */
fun distanceToSegmentMeters(
    pLat: Double, pLon: Double,
    aLat: Double, aLon: Double,
    bLat: Double, bLon: Double
): Float {
    val mPerDegLat = 111_320.0
    val meanLatRad = Math.toRadians((aLat + bLat) / 2.0)
    val mPerDegLon = 111_320.0 * Math.cos(meanLatRad)

    val px = (pLon - aLon) * mPerDegLon
    val py = (pLat - aLat) * mPerDegLat
    val bx = (bLon - aLon) * mPerDegLon
    val by = (bLat - aLat) * mPerDegLat

    val len2 = bx * bx + by * by
    val t = if (len2 == 0.0) 0.0 else ((px * bx + py * by) / len2).coerceIn(0.0, 1.0)
    val cx = t * bx
    val cy = t * by
    return Math.hypot(px - cx, py - cy).toFloat()
}
