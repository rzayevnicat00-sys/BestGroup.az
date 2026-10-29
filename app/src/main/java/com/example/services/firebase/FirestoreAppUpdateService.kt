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

    companion object {
        const val GITHUB_REPO_URL = "https://github.com/rzayevnicat00-sys/BestGroup.az"
        const val GITHUB_RELEASES_URL = "https://github.com/rzayevnicat00-sys/BestGroup.az/releases"
        const val GITHUB_LATEST_RELEASE_URL = "https://github.com/rzayevnicat00-sys/BestGroup.az/releases/latest"
        const val GITHUB_DIRECT_APK_URL = "https://github.com/rzayevnicat00-sys/BestGroup.az/releases/latest/download/BestGroup.az.apk"
    }

    /**
     * Real-time flow of the latest published application update.
     */
    fun getLatestUpdateFlow(): Flow<AppUpdateInfo?> = callbackFlow {
        val query = updatesCollection
            .orderBy("versionCode", Query.Direction.DESCENDING)
            .limit(1)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("AppUpdateService", "Error listening to app updates: ${error.message}")
                trySend(null)
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val doc = snapshot.documents.first()
                val updateInfo = mapDocToUpdate(doc.id, doc.data ?: emptyMap())
                trySend(updateInfo)
            } else {
                trySend(null)
            }
        }

        awaitClose { registration.remove() }
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
     * Publish or update an app release announcement in Firestore.
     */
    suspend fun publishUpdate(updateInfo: AppUpdateInfo): Result<String> {
        return try {
            val docId = if (updateInfo.id.isNotBlank()) updateInfo.id else "v_${updateInfo.versionCode}"
            val data = hashMapOf(
                "versionCode" to updateInfo.versionCode,
                "versionName" to updateInfo.versionName,
                "releaseNotesAz" to updateInfo.releaseNotesAz,
                "releaseNotesEn" to updateInfo.releaseNotesEn,
                "releaseNotesRu" to updateInfo.releaseNotesRu,
                "downloadUrl" to updateInfo.downloadUrl.ifBlank { GITHUB_DIRECT_APK_URL },
                "githubReleaseUrl" to updateInfo.githubReleaseUrl.ifBlank { GITHUB_RELEASES_URL },
                "isMandatory" to updateInfo.isMandatory,
                "minSupportedVersionCode" to updateInfo.minSupportedVersionCode,
                "publishedAtMillis" to if (updateInfo.publishedAtMillis > 0) updateInfo.publishedAtMillis else System.currentTimeMillis(),
                "publishedBy" to updateInfo.publishedBy,
                "apkSizeMb" to updateInfo.apkSizeMb
            )
            updatesCollection.document(docId).set(data).await()
            Result.success(docId)
        } catch (e: Exception) {
            Log.e("AppUpdateService", "Failed to publish update: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun mapDocToUpdate(id: String, data: Map<String, Any?>): AppUpdateInfo {
        return AppUpdateInfo(
            id = id,
            versionCode = (data["versionCode"] as? Number)?.toInt() ?: 1,
            versionName = data["versionName"] as? String ?: "1.0.0",
            releaseNotesAz = data["releaseNotesAz"] as? String ?: "",
            releaseNotesEn = data["releaseNotesEn"] as? String ?: "",
            releaseNotesRu = data["releaseNotesRu"] as? String ?: "",
            downloadUrl = data["downloadUrl"] as? String ?: GITHUB_DIRECT_APK_URL,
            githubReleaseUrl = data["githubReleaseUrl"] as? String ?: GITHUB_RELEASES_URL,
            isMandatory = data["isMandatory"] as? Boolean ?: false,
            minSupportedVersionCode = (data["minSupportedVersionCode"] as? Number)?.toInt() ?: 1,
            publishedAtMillis = (data["publishedAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            publishedBy = data["publishedBy"] as? String ?: "",
            apkSizeMb = (data["apkSizeMb"] as? Number)?.toDouble() ?: 0.0
        )
    }
}
