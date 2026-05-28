package com.bpo.gasapp.domain.model

enum class SortMode(val label: String) {
    PRICE("Precio"),
    DISTANCE("Distancia"),
    VALUE("Valor")
}

data class StationFilters(
    val fuel: FuelType = FuelType.GASOLINA_95,
    /** Max distance in km. Null = no distance limit. Requires user location. */
    val maxDistanceKm: Int? = 5,
    /** Empty = all brands. */
    val brands: Set<String> = emptySet(),
    val openNowOnly: Boolean = false,
    val sortMode: SortMode = SortMode.PRICE
) {
    companion object {
        val DISTANCE_OPTIONS = listOf(1, 5, 10, 25)
        const val DISTANCE_MIN_KM = 1
        const val DISTANCE_MAX_KM = 30
    }
}
