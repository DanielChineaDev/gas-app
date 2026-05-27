package com.bpo.gasapp.domain.model

data class Refuel(
    val id: Long = 0,
    val stationId: String?,
    val stationName: String,
    val fuel: FuelType,
    val liters: Double,
    val amount: Double,
    val timestamp: Long
) {
    val pricePerLiter: Double? get() = if (liters > 0) amount / liters else null
}

data class MonthlyStat(
    /** "yyyy-MM" */
    val month: String,
    val totalAmount: Double,
    val totalLiters: Double,
    val count: Int
)
