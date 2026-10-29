package com.example.model

data class StoryItem(
    val id: String,
    val createdBy: String,
    val creatorName: String = "BestGroup.az Rəsmi",
    val mediaUrl: String,
    val mediaType: String = "IMAGE", // "IMAGE" or "VIDEO"
    val caption: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
    val isActive: Boolean = true,
    val viewedUserIds: List<String> = emptyList()
) {
    val storyId: String get() = id
    val authorId: String get() = createdBy
    val authorName: String get() = creatorName
    val authorRole: String get() = "Rəsmi Təmsilçi"
    val imageUrl: String get() = mediaUrl
    val title: String get() = caption
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAtMillis

    fun isViewedBy(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return viewedUserIds.contains(userId)
    }
}
