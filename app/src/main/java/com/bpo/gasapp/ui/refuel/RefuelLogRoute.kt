package com.bpo.gasapp.ui.refuel

import android.net.Uri

object RefuelLogRoute {
    const val ARG_STATION_ID = "stationId"
    const val ARG_STATION_NAME = "stationName"
    const val ARG_FUEL = "fuel"
    const val PATTERN = "refuel?$ARG_STATION_ID={$ARG_STATION_ID}&$ARG_STATION_NAME={$ARG_STATION_NAME}&$ARG_FUEL={$ARG_FUEL}"

    fun build(stationId: String? = null, stationName: String? = null, fuel: String? = null): String =
        "refuel?$ARG_STATION_ID=${Uri.encode(stationId.orEmpty())}" +
            "&$ARG_STATION_NAME=${Uri.encode(stationName.orEmpty())}" +
            "&$ARG_FUEL=${Uri.encode(fuel.orEmpty())}"
}
