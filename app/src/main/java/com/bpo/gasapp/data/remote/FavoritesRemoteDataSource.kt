package com.bpo.gasapp.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private fun favoritesCollection() = auth.currentUser?.let { user ->
        firestore.collection("users").document(user.uid).collection("favorites")
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun fetchRemoteIds(): List<String> {
        val collection = favoritesCollection() ?: return emptyList()
        return runCatching {
            collection.get().await().documents.map { it.id }
        }.getOrDefault(emptyList())
    }

    suspend fun add(stationId: String) {
        val collection = favoritesCollection() ?: return
        runCatching {
            collection.document(stationId)
                .set(mapOf("addedAt" to System.currentTimeMillis()))
                .await()
        }
    }

    suspend fun remove(stationId: String) {
        val collection = favoritesCollection() ?: return
        runCatching { collection.document(stationId).delete().await() }
    }
}
