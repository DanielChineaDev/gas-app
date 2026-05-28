package com.bpo.gasapp.domain.model

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    /** 0f..1f progress toward unlocking. */
    val progress: Float
)
