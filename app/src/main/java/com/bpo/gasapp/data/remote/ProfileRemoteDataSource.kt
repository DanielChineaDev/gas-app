package com.bpo.gasapp.data.remote

import com.bpo.gasapp.domain.model.FuelType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private fun userDoc() = auth.currentUser?.let { firestore.collection("users").document(it.uid) }

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun getDefaultFuel(): FuelType? {
        val doc = userDoc() ?: return null
        return runCatching {
            val name = doc.get().await().getString(FIELD_FUEL)
            name?.let { FuelType.valueOf(it) }
        }.getOrNull()
    }

    suspend fun setDefaultFuel(fuel: FuelType) {
        val doc = userDoc() ?: return
        runCatching {
            doc.set(mapOf(FIELD_FUEL to fuel.name), SetOptions.merge()).await()
        }
    }

    private companion object {
        const val FIELD_FUEL = "defaultFuel"
    }
}
