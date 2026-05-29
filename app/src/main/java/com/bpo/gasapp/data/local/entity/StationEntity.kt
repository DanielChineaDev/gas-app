package com.bpo.gasapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val province: String,
    val schedule: String,
    val gasolina95: Double?,
    val gasolina98: Double?,
    val diesel: Double?,
    val dieselPremium: Double?,
    val glp: Double? = null,
    val gnc: Double? = null,
    val gnl: Double? = null,
    val hidrogeno: Double? = null,
    val adblue: Double? = null,
    val lastUpdate: Long
)
