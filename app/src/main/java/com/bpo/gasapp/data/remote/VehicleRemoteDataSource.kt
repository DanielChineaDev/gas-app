package com.bpo.gasapp.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Vehículo tal y como se guarda/lee en Firestore (por perfil de usuario). */
data class RemoteVehicle(
    val syncId: String,
    val name: String,
    val fuel: String,
    val consumption: Double
)

@Singleton
class VehicleRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private fun collection() = auth.currentUser?.let { user ->
        firestore.collection("users").document(user.uid).collection("vehicles")
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun fetchAll(): List<RemoteVehicle> {
        val collection = collection() ?: return emptyList()
        return runCatching {
            collection.get().await().documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                RemoteVehicle(
                    syncId = doc.id,
                    name = name,
                    fuel = doc.getString("fuel") ?: "GASOLINA_95",
                    consumption = doc.getDouble("consumption") ?: 0.0
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun put(vehicle: RemoteVehicle) {
        val collection = collection() ?: return
        runCatching {
            collection.document(vehicle.syncId).set(
                mapOf(
                    "name" to vehicle.name,
                    "fuel" to vehicle.fuel,
                    "consumption" to vehicle.consumption
                )
            ).await()
        }
    }

    suspend fun remove(syncId: String) {
        val collection = collection() ?: return
        if (syncId.isBlank()) return
        runCatching { collection.document(syncId).delete().await() }
    }
}
