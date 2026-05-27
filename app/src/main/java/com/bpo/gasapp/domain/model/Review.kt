package com.bpo.gasapp.domain.model

data class Review(
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long
)
