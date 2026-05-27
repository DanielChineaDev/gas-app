package com.bpo.gasapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "refuels")
data class RefuelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stationId: String?,
    val stationName: String,
    val fuel: String,
    val liters: Double,
    val amount: Double,
    val timestamp: Long
)
