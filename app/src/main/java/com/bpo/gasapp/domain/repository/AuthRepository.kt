package com.bpo.gasapp.domain.repository

import com.bpo.gasapp.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthUser?>
    fun currentUser(): AuthUser?
    suspend fun register(email: String, password: String): Result<AuthUser>
    suspend fun login(email: String, password: String): Result<AuthUser>
    fun logout()
}
