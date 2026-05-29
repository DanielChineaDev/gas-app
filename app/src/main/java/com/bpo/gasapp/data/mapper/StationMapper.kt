package com.bpo.gasapp.data.mapper

import com.bpo.gasapp.data.local.entity.StationEntity
import com.bpo.gasapp.data.remote.dto.EstacionDto
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.model.Station

/** Parses the API's comma-decimal strings ("1,459" -> 1.459). Empty -> null. */
private fun String.toSpanishDoubleOrNull(): Double? =
    trim().takeIf { it.isNotEmpty() }?.replace(',', '.')?.toDoubleOrNull()

fun EstacionDto.toEntity(updatedAt: Long): StationEntity? {
    val lat = latitud.toSpanishDoubleOrNull() ?: return null
    val lon = longitud.toSpanishDoubleOrNull() ?: return null
    if (id.isBlank()) return null
    return StationEntity(
        id = id,
        name = rotulo.trim().ifEmpty { "Gasolinera" },
        brand = rotulo.trim().ifEmpty { "Desconocida" },
        latitude = lat,
        longitude = lon,
        address = direccion.trim(),
        city = localidad.trim().ifEmpty { municipio.trim() },
        province = provincia.trim(),
        schedule = horario.trim(),
        gasolina95 = gasolina95.toSpanishDoubleOrNull(),
        gasolina98 = gasolina98.toSpanishDoubleOrNull(),
        diesel = diesel.toSpanishDoubleOrNull(),
        dieselPremium = dieselPremium.toSpanishDoubleOrNull(),
        glp = glp.toSpanishDoubleOrNull(),
        gnc = gnc.toSpanishDoubleOrNull(),
        gnl = gnl.toSpanishDoubleOrNull(),
        hidrogeno = hidrogeno.toSpanishDoubleOrNull(),
        adblue = adblue.toSpanishDoubleOrNull(),
        lastUpdate = updatedAt
    )
}

fun StationEntity.toDomain(isFavorite: Boolean = false): Station {
    val prices = buildMap {
        gasolina95?.let { put(FuelType.GASOLINA_95, it) }
        gasolina98?.let { put(FuelType.GASOLINA_98, it) }
        diesel?.let { put(FuelType.DIESEL, it) }
        dieselPremium?.let { put(FuelType.DIESEL_PREMIUM, it) }
        glp?.let { put(FuelType.GLP, it) }
        gnc?.let { put(FuelType.GNC, it) }
        gnl?.let { put(FuelType.GNL, it) }
        hidrogeno?.let { put(FuelType.HIDROGENO, it) }
        adblue?.let { put(FuelType.ADBLUE, it) }
    }
    return Station(
        id = id,
        name = name,
        brand = brand,
        latitude = latitude,
        longitude = longitude,
        address = address,
        city = city,
        province = province,
        schedule = schedule,
        prices = prices,
        lastUpdate = lastUpdate,
        isFavorite = isFavorite
    )
}
