package com.example.services.security

import android.util.Log
import com.example.model.LoginHistoryItem
import com.example.model.NotificationCategory
import com.example.model.NotificationItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class LoginHistoryService(private val firestore: FirebaseFirestore) {

    companion object {
        private const val TAG = "LoginHistoryService"
    }

    suspend fun recordLoginEvent(
        userId: String,
        deviceName: String,
        status: String = "Uğurlu giriş",
        isSuspicious: Boolean = false,
        eventType: String = "LOGIN",
        ipOrNetwork: String = "Wi-Fi / Mobil şəbəkə"
    ): Result<LoginHistoryItem> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID is required"))

        return try {
            val docId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val item = LoginHistoryItem(
                id = docId,
                userId = userId,
                deviceName = deviceName,
                timestamp = now,
                status = status,
                ipOrNetwork = ipOrNetwork,
                isSuspicious = isSuspicious,
                eventType = eventType
            )

            val map = mapOf(
                "id" to item.id,
                "userId" to item.userId,
                "deviceName" to item.deviceName,
                "timestamp" to item.timestamp,
                "status" to item.status,
                "ipOrNetwork" to item.ipOrNetwork,
                "isSuspicious" to item.isSuspicious,
                "eventType" to item.eventType
            )

            firestore.collection("users").document(userId)
                .collection("login_history").document(docId)
                .set(map)
                .await()

            Result.success(item)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record login history: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getLoginHistoryFlow(userId: String): Flow<List<LoginHistoryItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("users").document(userId)
            .collection("login_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Failed to listen to login history: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        LoginHistoryItem(
                            id = doc.id,
                            userId = userId,
                            deviceName = data["deviceName"] as? String ?: "Cihaz",
                            timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                            status = data["status"] as? String ?: "Uğurlu giriş",
                            ipOrNetwork = data["ipOrNetwork"] as? String ?: "Şəbəkə",
                            isSuspicious = data["isSuspicious"] as? Boolean ?: false,
                            eventType = data["eventType"] as? String ?: "LOGIN"
                        )
                    }
                    trySend(list)
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun checkAndAlertIfNewDevice(
        userId: String,
        currentDeviceName: String,
        currentSessionId: String
    ): Boolean {
        if (userId.isBlank()) return false
        return try {
            val sessionsSnapshot = firestore.collection("users").document(userId)
                .collection("sessions")
                .get()
                .await()

            // If there were already multiple sessions registered before this one
            val otherSessions = sessionsSnapshot.documents.filter { it.id != currentSessionId }
            val hasSeenDevice = otherSessions.any { doc ->
                val name = doc.getString("deviceName") ?: ""
                name.equals(currentDeviceName, ignoreCase = true)
            }

            if (!hasSeenDevice && otherSessions.isNotEmpty()) {
                // New device detected on an existing account! Create security notification
                val notifId = "sec_alert_${System.currentTimeMillis()}"
                val notifData = mapOf(
                    "id" to notifId,
                    "title" to "Yeni Cihazdan Giriş Aşkarlanmışdır",
                    "body" to "Hesabınıza yeni cihazdan ('$currentDeviceName') giriş edildi. Əgər bu siz deyilsinizsə, dərhal şifrənizi dəyişin və digər aktiv cihazları bağlayın.",
                    "category" to NotificationCategory.SYSTEM.name,
                    "timestamp" to "İndicə",
                    "createdAt" to System.currentTimeMillis(),
                    "isRead" to false
                )

                firestore.collection("users").document(userId)
                    .collection("notifications").document(notifId)
                    .set(notifData)
                    .await()

                recordLoginEvent(
                    userId = userId,
                    deviceName = currentDeviceName,
                    status = "Yeni cihaz aşkarlanması",
                    isSuspicious = true,
                    eventType = "NEW_DEVICE_ALERT"
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "checkAndAlertIfNewDevice warning: ${e.message}")
            false
        }
    }
}
