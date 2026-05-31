package com.bpo.gasapp.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Repostaje tal y como se guarda/lee en Firestore (por perfil de usuario). */
data class RemoteRefuel(
    val syncId: String,
    val stationId: String?,
    val stationName: String,
    val fuel: String,
    val liters: Double,
    val amount: Double,
    val odometer: Double?,
    /** syncId del vehículo asociado (para enlazar entre dispositivos). */
    val vehicleSyncId: String?,
    val timestamp: Long
)

@Singleton
class RefuelRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private fun collection() = auth.currentUser?.let { user ->
        firestore.collection("users").document(user.uid).collection("refuels")
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun fetchAll(): List<RemoteRefuel> {
        val collection = collection() ?: return emptyList()
        return runCatching {
            collection.get().await().documents.mapNotNull { doc ->
                val stationName = doc.getString("stationName") ?: return@mapNotNull null
                RemoteRefuel(
                    syncId = doc.id,
                    stationId = doc.getString("stationId"),
                    stationName = stationName,
                    fuel = doc.getString("fuel") ?: "GASOLINA_95",
                    liters = doc.getDouble("liters") ?: 0.0,
                    amount = doc.getDouble("amount") ?: 0.0,
                    odometer = doc.getDouble("odometer"),
                    vehicleSyncId = doc.getString("vehicleSyncId"),
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun put(refuel: RemoteRefuel) {
        val collection = collection() ?: return
        val data = hashMapOf<String, Any?>(
            "stationId" to refuel.stationId,
            "stationName" to refuel.stationName,
            "fuel" to refuel.fuel,
            "liters" to refuel.liters,
            "amount" to refuel.amount,
            "odometer" to refuel.odometer,
            "vehicleSyncId" to refuel.vehicleSyncId,
            "timestamp" to refuel.timestamp
        )
        runCatching { collection.document(refuel.syncId).set(data).await() }
    }

    suspend fun remove(syncId: String) {
        val collection = collection() ?: return
        if (syncId.isBlank()) return
        runCatching { collection.document(syncId).delete().await() }
    }
}
