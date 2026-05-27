package com.bpo.gasapp.data.remote

import com.bpo.gasapp.domain.model.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private fun items(stationId: String) =
        firestore.collection("reviews").document(stationId).collection("items")

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun observeReviews(stationId: String): Flow<List<Review>> = callbackFlow {
        val registration = items(stationId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    val rating = (doc.getLong("rating") ?: return@mapNotNull null).toInt()
                    Review(
                        userId = doc.id,
                        userName = doc.getString("userName") ?: "Usuario",
                        rating = rating,
                        comment = doc.getString("comment").orEmpty(),
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }.orEmpty()
                trySend(reviews)
            }
        awaitClose { registration.remove() }
    }

    suspend fun submitReview(stationId: String, rating: Int, comment: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Inicia sesión para reseñar")
        val data = mapOf(
            "userName" to (user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "Usuario"),
            "rating" to rating,
            "comment" to comment.trim(),
            "timestamp" to System.currentTimeMillis()
        )
        items(stationId).document(user.uid).set(data).await()
    }
}
