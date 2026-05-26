package com.bpo.gasapp.domain.model

data class Station(
    val id: String,
    val name: String,
    val brand: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val province: String,
    val schedule: String,
    val prices: Map<FuelType, Double>,
    val lastUpdate: Long,
    /** Distance to the user in meters, filled at query time. Null if unknown. */
    val distanceMeters: Float? = null,
    val isFavorite: Boolean = false
) {
    fun priceOf(fuel: FuelType): Double? = prices[fuel]
}
