package com.bpo.gasapp.domain.model

data class StationFilters(
    val fuel: FuelType = FuelType.GASOLINA_95,
    /** Max distance in km. Null = no distance limit. Requires user location. */
    val maxDistanceKm: Int? = null,
    /** Empty = all brands. */
    val brands: Set<String> = emptySet(),
    val openNowOnly: Boolean = false
) {
    companion object {
        val DISTANCE_OPTIONS = listOf(1, 5, 10, 25)
    }
}
