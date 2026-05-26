package com.bpo.gasapp.domain.model

data class PriceDrop(
    val stationId: String,
    val stationName: String,
    val fuel: FuelType,
    val oldPrice: Double,
    val newPrice: Double
)
