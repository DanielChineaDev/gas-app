package com.bpo.gasapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val fuel: String,
    val consumption: Double,
    /** Identificador estable entre dispositivos (id del documento en Firestore). */
    @ColumnInfo(defaultValue = "''") val syncId: String = ""
)
