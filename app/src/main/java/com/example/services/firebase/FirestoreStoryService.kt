package com.example.services.firebase

import com.example.model.StoryItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreStoryService(
    private val firestore: FirebaseFirestore
) {
    private val storiesCollection = firestore.collection("stories")

    fun getActiveStoriesFlow(): Flow<List<StoryItem>> = callbackFlow {
        val now = System.currentTimeMillis()
        val query = storiesCollection
            .whereEqualTo("isActive", true)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val currentNow = System.currentTimeMillis()
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val expiresAt = (data["expiresAtMillis"] as? Number)?.toLong()
                        ?: (data["expiresAt"] as? Number)?.toLong()
                        ?: (currentNow + 24 * 60 * 60 * 1000L)

                    // Skip expired stories
                    if (expiresAt < currentNow) return@mapNotNull null

                    val viewedList = (data["viewedUserIds"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                    StoryItem(
                        id = doc.id,
                        createdBy = data["createdBy"] as? String ?: "",
                        creatorName = data["creatorName"] as? String ?: "BestGroup.az",
                        mediaUrl = data["mediaUrl"] as? String ?: "",
                        mediaType = data["mediaType"] as? String ?: "IMAGE",
                        caption = data["caption"] as? String ?: "",
                        createdAtMillis = (data["createdAtMillis"] as? Number)?.toLong() ?: currentNow,
                        expiresAtMillis = expiresAt,
                        isActive = (data["isActive"] as? Boolean) ?: true,
                        viewedUserIds = viewedList
                    )
                }.sortedBy { it.createdAtMillis }
                trySend(list)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun publishStory(
        createdBy: String,
        creatorName: String = "BestGroup.az Rəsmi",
        mediaUrl: String,
        mediaType: String = "IMAGE",
        caption: String = "",
        durationHours: Int = 24
    ): StoryItem {
        require(createdBy.isNotBlank()) { "Müəllif təyin edilməyib." }
        require(mediaUrl.isNotBlank()) { "Media URL boş ola bilməz." }

        val storyId = "story_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val expiresAt = now + (durationHours * 60 * 60 * 1000L)

        val data = hashMapOf(
            "storyId" to storyId,
            "createdBy" to createdBy,
            "creatorName" to creatorName,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "caption" to caption.trim(),
            "createdAtMillis" to now,
            "expiresAtMillis" to expiresAt,
            "isActive" to true,
            "viewedUserIds" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAt
        )

        storiesCollection.document(storyId).set(data).await()

        return StoryItem(
            id = storyId,
            createdBy = createdBy,
            creatorName = creatorName,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            caption = caption.trim(),
            createdAtMillis = now,
            expiresAtMillis = expiresAt,
            isActive = true,
            viewedUserIds = emptyList()
        )
    }

    suspend fun publishStory(story: StoryItem): StoryItem {
        val data = hashMapOf(
            "storyId" to story.id,
            "createdBy" to story.createdBy,
            "creatorName" to story.creatorName,
            "mediaUrl" to story.mediaUrl,
            "mediaType" to story.mediaType,
            "caption" to story.caption.trim(),
            "createdAtMillis" to story.createdAtMillis,
            "expiresAtMillis" to story.expiresAtMillis,
            "isActive" to story.isActive,
            "viewedUserIds" to story.viewedUserIds,
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to story.expiresAtMillis
        )
        storiesCollection.document(story.id).set(data).await()
        return story
    }

    suspend fun markStoryViewed(storyId: String, userId: String) {
        if (storyId.isBlank() || userId.isBlank()) return
        try {
            storiesCollection.document(storyId).update(
                "viewedUserIds", FieldValue.arrayUnion(userId)
            ).await()
        } catch (_: Exception) {}
    }

    suspend fun deleteStory(storyId: String) {
        if (storyId.isBlank()) return
        try {
            // Hard delete or soft deactivate
            storiesCollection.document(storyId).delete().await()
        } catch (_: Exception) {
            try {
                storiesCollection.document(storyId).update("isActive", false).await()
            } catch (_: Exception) {}
        }
    }
}
