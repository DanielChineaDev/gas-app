package com.bpo.gasapp.data.repository

import com.bpo.gasapp.domain.model.AuthUser
import com.bpo.gasapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val authState: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun currentUser(): AuthUser? = auth.currentUser?.toAuthUser()

    override suspend fun register(email: String, password: String): Result<AuthUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        result.user?.toAuthUser() ?: error("Usuario nulo tras el registro")
    }

    override suspend fun login(email: String, password: String): Result<AuthUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        result.user?.toAuthUser() ?: error("Usuario nulo tras el inicio de sesión")
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthUser> = runCatching {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        result.user?.toAuthUser() ?: error("Usuario nulo tras el inicio con Google")
    }

    override suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("No hay sesión activa")
        val request = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(name.trim())
            .build()
        user.updateProfile(request).await()
        user.reload().await()
    }

    override fun logout() = auth.signOut()
}

private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email, displayName = displayName)
