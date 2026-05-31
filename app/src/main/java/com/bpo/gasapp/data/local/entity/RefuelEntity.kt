package com.bpo.gasapp.data.local.entity

import androidx.room.ColumnInfo
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
    val odometer: Double?,
    val vehicleId: Long?,
    val timestamp: Long,
    /** Identificador estable entre dispositivos (id del documento en Firestore). */
    @ColumnInfo(defaultValue = "''") val syncId: String = ""
)
