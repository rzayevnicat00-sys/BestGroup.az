package com.example.services.security

import android.util.Log
import com.example.model.AdminAuditLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminAuditService(private val firestore: FirebaseFirestore) {

    companion object {
        private const val TAG = "AdminAuditService"
        private const val COLLECTION_NAME = "admin_audit_logs"
    }

    suspend fun logAction(
        adminId: String,
        adminEmail: String,
        action: String,
        targetUserId: String? = null,
        details: String = ""
    ): Result<AdminAuditLog> {
        return try {
            val docId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val log = AdminAuditLog(
                id = docId,
                adminId = adminId,
                adminEmail = adminEmail,
                action = action,
                targetUserId = targetUserId,
                details = details,
                timestamp = now
            )

            val map = mapOf(
                "id" to log.id,
                "adminId" to log.adminId,
                "adminEmail" to log.adminEmail,
                "action" to log.action,
                "targetUserId" to log.targetUserId,
                "details" to log.details,
                "timestamp" to log.timestamp
            )

            firestore.collection(COLLECTION_NAME).document(docId).set(map).await()
            Result.success(log)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write admin audit log: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getAuditLogsFlow(): Flow<List<AdminAuditLog>> = callbackFlow {
        val registration = firestore.collection(COLLECTION_NAME)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Failed to load audit logs: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        AdminAuditLog(
                            id = doc.id,
                            adminId = data["adminId"] as? String ?: "",
                            adminEmail = data["adminEmail"] as? String ?: "",
                            action = data["action"] as? String ?: "",
                            targetUserId = data["targetUserId"] as? String,
                            details = data["details"] as? String ?: "",
                            timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                        )
                    }
                    trySend(logs)
                }
            }

        awaitClose { registration.remove() }
    }
}
