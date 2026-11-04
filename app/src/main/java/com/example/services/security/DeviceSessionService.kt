package com.example.services.security

import android.os.Build
import android.util.Log
import com.example.model.DeviceSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DeviceSessionService(private val firestore: FirebaseFirestore) {

    private var currentSessionListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "DeviceSessionService"

        fun getDeviceDisplayName(): String {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val model = Build.MODEL
            return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        }

        fun getOsDisplayName(): String {
            return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        }
    }

    suspend fun registerOrUpdateSession(
        userId: String,
        sessionId: String,
        appVersion: String = "2.0.0",
        ipAddress: String = "Wi-Fi / Mobil şəbəkə",
        location: String = "Bakı, Azərbaycan"
    ): Result<DeviceSession> {
        if (userId.isBlank() || sessionId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID and Session ID must not be blank"))
        }

        return try {
            val sessionRef = firestore.collection("users").document(userId)
                .collection("sessions").document(sessionId)

            val existingSnapshot = sessionRef.get().await()
            val now = System.currentTimeMillis()

            val session = if (existingSnapshot.exists()) {
                val data = existingSnapshot.data ?: emptyMap()
                DeviceSession(
                    id = sessionId,
                    sessionId = sessionId,
                    userId = userId,
                    deviceName = data["deviceName"] as? String ?: getDeviceDisplayName(),
                    deviceModel = data["deviceModel"] as? String ?: Build.DEVICE,
                    osVersion = data["osVersion"] as? String ?: getOsDisplayName(),
                    appVersion = appVersion,
                    ipAddress = ipAddress,
                    location = location,
                    loginTimestamp = (data["loginTimestamp"] as? Number)?.toLong() ?: now,
                    lastActiveTimestamp = now,
                    isCurrentDevice = true,
                    isRevoked = false
                )
            } else {
                DeviceSession(
                    id = sessionId,
                    sessionId = sessionId,
                    userId = userId,
                    deviceName = getDeviceDisplayName(),
                    deviceModel = Build.DEVICE,
                    osVersion = getOsDisplayName(),
                    appVersion = appVersion,
                    ipAddress = ipAddress,
                    location = location,
                    loginTimestamp = now,
                    lastActiveTimestamp = now,
                    isCurrentDevice = true,
                    isRevoked = false
                )
            }

            val map = mapOf(
                "sessionId" to session.sessionId,
                "userId" to session.userId,
                "deviceName" to session.deviceName,
                "deviceModel" to session.deviceModel,
                "osVersion" to session.osVersion,
                "appVersion" to session.appVersion,
                "ipAddress" to session.ipAddress,
                "location" to session.location,
                "loginTimestamp" to session.loginTimestamp,
                "lastActiveTimestamp" to session.lastActiveTimestamp,
                "isRevoked" to false,
                "updatedAt" to now
            )

            sessionRef.set(map, SetOptions.merge()).await()
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering session: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun startListeningToCurrentSession(
        userId: String,
        sessionId: String,
        onSessionRevoked: () -> Unit
    ) {
        currentSessionListener?.remove()
        if (userId.isBlank() || sessionId.isBlank()) return

        currentSessionListener = firestore.collection("users").document(userId)
            .collection("sessions").document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Session listener warning: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val isRevoked = snapshot.getBoolean("isRevoked") ?: false
                    if (isRevoked) {
                        Log.i(TAG, "Current device session was revoked on backend! Triggering force logout.")
                        onSessionRevoked()
                    }
                }
            }
    }

    fun stopListeningToCurrentSession() {
        currentSessionListener?.remove()
        currentSessionListener = null
    }

    fun getActiveSessionsFlow(userId: String, currentSessionId: String): Flow<List<DeviceSession>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("users").document(userId)
            .collection("sessions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Failed to load active sessions: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val sessions = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val sId = doc.id
                        val isRevoked = data["isRevoked"] as? Boolean ?: false
                        if (isRevoked) return@mapNotNull null

                        DeviceSession(
                            id = sId,
                            sessionId = sId,
                            userId = userId,
                            deviceName = data["deviceName"] as? String ?: "Android Cihaz",
                            deviceModel = data["deviceModel"] as? String ?: "",
                            osVersion = data["osVersion"] as? String ?: "Android",
                            appVersion = data["appVersion"] as? String ?: "2.0.0",
                            ipAddress = data["ipAddress"] as? String ?: "127.0.0.1",
                            location = data["location"] as? String ?: "Bakı, Azərbaycan",
                            loginTimestamp = (data["loginTimestamp"] as? Number)?.toLong() ?: 0L,
                            lastActiveTimestamp = (data["lastActiveTimestamp"] as? Number)?.toLong() ?: 0L,
                            isCurrentDevice = (sId == currentSessionId),
                            isRevoked = isRevoked
                        )
                    }.sortedWith(compareByDescending<DeviceSession> { it.isCurrentDevice }.thenByDescending { it.lastActiveTimestamp })

                    trySend(sessions)
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun revokeSession(userId: String, sessionId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("sessions").document(sessionId)
                .set(mapOf("isRevoked" to true, "revokedAt" to System.currentTimeMillis()), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Revoke session error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun revokeAllOtherSessions(userId: String, currentSessionId: String): Result<Int> {
        return try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("sessions")
                .get()
                .await()

            var count = 0
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                if (doc.id != currentSessionId) {
                    batch.set(doc.reference, mapOf("isRevoked" to true, "revokedAt" to System.currentTimeMillis()), SetOptions.merge())
                    count++
                }
            }
            batch.commit().await()
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Revoke all other sessions error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun revokeAllSessions(userId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("sessions")
                .get()
                .await()

            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.set(doc.reference, mapOf("isRevoked" to true, "revokedAt" to System.currentTimeMillis()), SetOptions.merge())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Revoke all sessions error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
