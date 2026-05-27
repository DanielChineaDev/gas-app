package com.bpo.gasapp.domain.model

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String? = null
)
