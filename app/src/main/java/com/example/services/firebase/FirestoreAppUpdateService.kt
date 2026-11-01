package com.example.services.firebase

import android.util.Log
import com.example.model.AppUpdateInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreAppUpdateService(
    private val firestore: FirebaseFirestore
) {
    private val updatesCollection = firestore.collection("app_updates")
    private val appConfigDoc = firestore.collection("app_config").document("update")

    companion object {
        const val GITHUB_REPO_URL = "https://github.com/rzayevnicat00-sys/BestGroup.az"
        const val GITHUB_RELEASES_URL = "https://github.com/rzayevnicat00-sys/BestGroup.az/releases"
        const val GITHUB_LATEST_RELEASE_URL = "https://github.com/rzayevnicat00-sys/BestGroup.az/releases/latest"
        const val GITHUB_DIRECT_APK_URL = "https://github.com/rzayevnicat00-sys/BestGroup.az/releases/latest/download/BestGroup.az.apk"
    }

    /**
     * Real-time flow of the latest published application update from app_config/update,
     * with graceful fallback to app_updates collection.
     */
    fun getLatestUpdateFlow(): Flow<AppUpdateInfo?> = callbackFlow {
        val configReg = appConfigDoc.addSnapshotListener { configSnap, configErr ->
            if (configErr != null) {
                Log.w("AppUpdateService", "Error listening to app_config/update: ${configErr.message}")
            }

            if (configSnap != null && configSnap.exists() && configSnap.data != null) {
                val data = configSnap.data!!
                val updateInfo = mapConfigDocToUpdate(data)
                trySend(updateInfo)
                return@addSnapshotListener
            }

            // Fallback: check app_updates collection
            updatesCollection
                .orderBy("versionCode", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val doc = snapshot.documents.first()
                        trySend(mapDocToUpdate(doc.id, doc.data ?: emptyMap()))
                    } else {
                        trySend(null)
                    }
                }
                .addOnFailureListener {
                    trySend(null)
                }
        }

        awaitClose { configReg.remove() }
    }

    /**
     * Real-time flow of all application updates (for Admin panel history).
     */
    fun getAllUpdatesFlow(): Flow<List<AppUpdateInfo>> = callbackFlow {
        val query = updatesCollection
            .orderBy("versionCode", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("AppUpdateService", "Error listening to all app updates: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    mapDocToUpdate(doc.id, doc.data ?: emptyMap())
                }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { registration.remove() }
    }

    /**
     * Publish or update an app release in Firestore:
     * Writes to app_config/update document (as required by BestGroup.az 2.0 specification)
     * and records in app_updates for release history.
     */
    suspend fun publishUpdate(updateInfo: AppUpdateInfo): Result<String> {
        return try {
            val now = System.currentTimeMillis()
            val effectiveUrl = updateInfo.effectiveDownloadUrl
            val effectiveNotes = updateInfo.effectiveReleaseNotes
            val code = updateInfo.versionCode
            val name = updateInfo.versionName
            val isForce = updateInfo.isMandatory
            val minCode = updateInfo.minSupportedVersionCode

            // 1. Primary: app_config/update
            val configData = hashMapOf(
                "latestVersionCode" to code.toLong(),
                "latestVersionName" to name,
                "apkDownloadUrl" to effectiveUrl,
                "releaseNotes" to effectiveNotes,
                "isUpdateAvailable" to updateInfo.isUpdateAvailable,
                "isForceUpdate" to isForce,
                "minimumSupportedVersionCode" to minCode.toLong(),
                "updatedAt" to now
            )
            appConfigDoc.set(configData).await()

            // 2. Historical: app_updates/v_{code}
            val historyDocId = "v_$code"
            val historyData = hashMapOf(
                "versionCode" to code,
                "versionName" to name,
                "releaseNotesAz" to effectiveNotes,
                "downloadUrl" to effectiveUrl,
                "githubReleaseUrl" to updateInfo.githubReleaseUrl.ifBlank { GITHUB_RELEASES_URL },
                "isMandatory" to isForce,
                "minSupportedVersionCode" to minCode,
                "publishedAtMillis" to now,
                "publishedBy" to updateInfo.publishedBy,
                "apkSizeMb" to updateInfo.apkSizeMb
            )
            updatesCollection.document(historyDocId).set(historyData).await()

            Result.success("update")
        } catch (e: Exception) {
            Log.e("AppUpdateService", "Failed to publish update: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun mapConfigDocToUpdate(data: Map<String, Any?>): AppUpdateInfo {
        val latestCode = (data["latestVersionCode"] as? Number)?.toLong() ?: 1L
        val latestName = data["latestVersionName"] as? String ?: "1.0.0"
        val downloadUrl = data["apkDownloadUrl"] as? String ?: GITHUB_DIRECT_APK_URL
        val notes = data["releaseNotes"] as? String ?: ""
        val isAvailable = (data["isUpdateAvailable"] as? Boolean) ?: true
        val isForce = (data["isForceUpdate"] as? Boolean) ?: false
        val minCode = (data["minimumSupportedVersionCode"] as? Number)?.toLong() ?: 1L
        val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

        return AppUpdateInfo(
            id = "update",
            latestVersionCode = latestCode,
            latestVersionName = latestName,
            apkDownloadUrl = downloadUrl,
            releaseNotes = notes,
            isUpdateAvailable = isAvailable,
            isForceUpdate = isForce,
            minimumSupportedVersionCode = minCode,
            updatedAt = updatedAt,
            legacyVersionCode = latestCode.toInt(),
            legacyVersionName = latestName,
            releaseNotesAz = notes,
            downloadUrl = downloadUrl,
            legacyIsMandatory = isForce,
            legacyMinSupportedVersionCode = minCode.toInt()
        )
    }

    private fun mapDocToUpdate(id: String, data: Map<String, Any?>): AppUpdateInfo {
        val vCode = (data["versionCode"] as? Number)?.toInt() ?: 1
        val vName = data["versionName"] as? String ?: "1.0.0"
        val dUrl = data["downloadUrl"] as? String ?: GITHUB_DIRECT_APK_URL
        val notes = data["releaseNotesAz"] as? String ?: (data["releaseNotes"] as? String ?: "")
        val isMand = (data["isMandatory"] as? Boolean) ?: (data["isForceUpdate"] as? Boolean ?: false)
        val minCode = (data["minSupportedVersionCode"] as? Number)?.toInt() ?: 1

        return AppUpdateInfo(
            id = id,
            latestVersionCode = vCode.toLong(),
            latestVersionName = vName,
            apkDownloadUrl = dUrl,
            releaseNotes = notes,
            isUpdateAvailable = true,
            isForceUpdate = isMand,
            minimumSupportedVersionCode = minCode.toLong(),
            legacyVersionCode = vCode,
            legacyVersionName = vName,
            releaseNotesAz = notes,
            downloadUrl = dUrl,
            githubReleaseUrl = data["githubReleaseUrl"] as? String ?: GITHUB_RELEASES_URL,
            legacyIsMandatory = isMand,
            legacyMinSupportedVersionCode = minCode,
            publishedAtMillis = (data["publishedAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            publishedBy = data["publishedBy"] as? String ?: "",
            apkSizeMb = (data["apkSizeMb"] as? Number)?.toDouble() ?: 31.0
        )
    }
}
