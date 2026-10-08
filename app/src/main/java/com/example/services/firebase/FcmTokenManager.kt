package com.example.services.firebase

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FcmTokenManager(
    private val context: Context,
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }
    },
    private val authProvider: () -> FirebaseAuth? = {
        try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    }
) {

    private fun getDeviceId(): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrBlank()) androidId else "device_unknown"
        } catch (_: Exception) {
            "device_default"
        }
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    suspend fun registerCurrentToken(userId: String? = null) {
        val auth = authProvider() ?: return
        val uid = userId ?: auth.currentUser?.uid ?: return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNullOrBlank()) return
            saveToken(uid, token)
        } catch (e: Exception) {
            // Non-blocking error handling
        }
    }

    suspend fun saveToken(userId: String, token: String) {
        val firestore = firestoreProvider() ?: return
        if (userId.isBlank() || token.isBlank()) return
        val deviceId = getDeviceId()
        val appVersion = getAppVersion()

        val deviceData = hashMapOf(
            "token" to token,
            "platform" to "android",
            "deviceId" to deviceId,
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "osVersion" to Build.VERSION.RELEASE,
            "appVersion" to appVersion,
            "active" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            firestore.collection("users")
                .document(userId)
                .collection("devices")
                .document(deviceId)
                .set(deviceData, SetOptions.merge())
                .await()
        } catch (_: Exception) {
            // Fails silently if offline or permissions issue
        }
    }

    suspend fun deactivateToken(userId: String? = null) {
        val firestore = firestoreProvider() ?: return
        val auth = authProvider()
        val uid = userId ?: auth?.currentUser?.uid ?: return
        val deviceId = getDeviceId()

        try {
            firestore.collection("users")
                .document(uid)
                .collection("devices")
                .document(deviceId)
                .update(
                    mapOf(
                        "active" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
        } catch (_: Exception) {
            // Best effort on logout
        }
    }
}
