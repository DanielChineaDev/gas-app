package com.bpo.gasapp.ui.map

import com.bpo.gasapp.domain.model.Station
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class StationClusterItem(
    val station: Station,
    val markerLabel: String,
    private val snippetText: String?
) : ClusterItem {
    private val position = LatLng(station.latitude, station.longitude)

    override fun getPosition(): LatLng = position
    override fun getTitle(): String = station.brand
    override fun getSnippet(): String? = snippetText
    override fun getZIndex(): Float = 0f
}
