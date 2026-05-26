package com.bpo.gasapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val stationId: String,
    val addedAt: Long = System.currentTimeMillis()
)
