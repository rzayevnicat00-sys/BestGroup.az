package com.example.model

enum class NotificationCategory {
    ALL,
    ORDERS,
    MESSAGES,
    SYSTEM,
    CAMPAIGNS
}

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val category: NotificationCategory,
    val timestamp: String,
    val isRead: Boolean = false,
    val targetOrderId: String? = null,
    val targetChatId: String? = null
)
